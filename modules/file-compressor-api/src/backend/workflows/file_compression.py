"""
File compression workflows using Temporal.

This demonstrates asynchronous file compression with:
1. Progress tracking through database updates
2. Multiple compression formats (ZIP, TAR.GZ)
3. Error handling and retries
4. Clean separation between orchestration and execution
"""

import asyncio
import base64
import gzip
import io
import zipfile
from datetime import timedelta
from typing import List, Optional

from pydantic import BaseModel
from temporalio import activity, workflow
from temporalio.exceptions import ApplicationError


class FileItem(BaseModel):
    """Represents a single file to be compressed"""
    name: str
    content: str  # Base64 encoded content
    size: int


class CompressionJobInput(BaseModel):
    """Input parameters for the compression workflow"""
    job_id: str
    files: List[FileItem]
    compression_format: str = "zip"  # zip, tar_gz
    compression_level: int = 6  # 0-9 for gzip, 0-9 for zip


class CompressionJobResult(BaseModel):
    """Result of the compression workflow"""
    job_id: str
    compressed_data: str  # Base64 encoded compressed data
    original_size: int
    compressed_size: int
    compression_ratio: float
    status: str


class ProgressUpdate(BaseModel):
    """Progress update for the compression job"""
    job_id: str
    progress: int  # 0-100
    status: str
    message: Optional[str] = None


@activity.defn
async def update_job_progress(progress: ProgressUpdate) -> None:
    """
    Activity to update job progress in the database.
    
    This allows the frontend to track compression progress in real-time.
    """
    activity.logger.info(f"Updating progress for job {progress.job_id}: {progress.progress}%")
    
    # Get database connection for activity
    # Activities need their own database connection separate from the main app
    from ..clients.postgres import PostgresClient
    from .. import conf
    from ..db.models import update_compression_job_progress, complete_compression_job
    
    try:
        # Create a separate database connection for this activity
        postgres_config = conf.get_postgres_conf()
        pool_config = conf.get_postgres_pool_conf()
        postgres_client = PostgresClient(postgres_config, pool_config)
        
        await postgres_client.initialize()
        await postgres_client.init_connection()
        
        # Get a database session
        async with postgres_client.get_session() as session:
            await update_compression_job_progress(
                session, 
                progress.job_id, 
                progress.progress, 
                progress.status,
                progress.message
            )
            
        await postgres_client.close()
        
    except Exception as e:
        activity.logger.error(f"Failed to update progress in database: {e}")
        # Don't fail the workflow for database update issues
        pass


@activity.defn
def compress_files_zip(input: CompressionJobInput) -> bytes:
    """
    Activity to compress files into a ZIP archive.
    
    This performs the actual compression work.
    """
    activity.logger.info(f"Compressing {len(input.files)} files to ZIP format")
    
    zip_buffer = io.BytesIO()
    
    with zipfile.ZipFile(zip_buffer, 'w', zipfile.ZIP_DEFLATED, compresslevel=input.compression_level) as zip_file:
        for file_item in input.files:
            # Decode base64 content
            try:
                file_content = base64.b64decode(file_item.content)
            except Exception as e:
                raise ApplicationError(f"Failed to decode file {file_item.name}: {str(e)}")
            
            # Add file to ZIP
            zip_file.writestr(file_item.name, file_content)
    
    zip_buffer.seek(0)
    return zip_buffer.read()


@activity.defn
def compress_files_tar_gz(input: CompressionJobInput) -> bytes:
    """
    Activity to compress files into a TAR.GZ archive.
    """
    activity.logger.info(f"Compressing {len(input.files)} files to TAR.GZ format")
    
    # For simplicity, we'll create a simple gzip-compressed concatenated file
    # In a real implementation, you'd use the tarfile module
    
    combined_content = io.BytesIO()
    
    for file_item in input.files:
        try:
            file_content = base64.b64decode(file_item.content)
            combined_content.write(f"--- {file_item.name} ---\n".encode())
            combined_content.write(file_content)
            combined_content.write(b"\n\n")
        except Exception as e:
            raise ApplicationError(f"Failed to decode file {file_item.name}: {str(e)}")
    
    combined_content.seek(0)
    compressed_data = gzip.compress(combined_content.read(), compresslevel=input.compression_level)
    
    return compressed_data


@workflow.defn
class FileCompressionWorkflow:
    """
    Workflow that orchestrates file compression with progress tracking.
    
    This workflow:
    1. Updates progress as files are processed
    2. Performs the actual compression
    3. Calculates compression statistics
    4. Updates the final status
    """

    @workflow.run
    async def run(self, input: CompressionJobInput) -> CompressionJobResult:
        """
        Main workflow execution logic.
        """
        workflow.logger.info(f"Starting compression workflow for job {input.job_id}")
        
        try:
            # Update initial status
            await workflow.execute_activity(
                update_job_progress,
                ProgressUpdate(
                    job_id=input.job_id,
                    progress=0,
                    status="starting",
                    message="Initializing compression"
                ),
                start_to_close_timeout=timedelta(seconds=10),
            )
            
            # Calculate total original size
            original_size = sum(file.size for file in input.files)
            
            # Update progress - preparing
            await workflow.execute_activity(
                update_job_progress,
                ProgressUpdate(
                    job_id=input.job_id,
                    progress=25,
                    status="preparing",
                    message="Preparing files for compression"
                ),
                start_to_close_timeout=timedelta(seconds=10),
            )
            
            # Simulate some processing time
            await workflow.sleep(1)
            
            # Update progress - compressing
            await workflow.execute_activity(
                update_job_progress,
                ProgressUpdate(
                    job_id=input.job_id,
                    progress=50,
                    status="compressing",
                    message="Compressing files"
                ),
                start_to_close_timeout=timedelta(seconds=10),
            )
            
            # Perform compression based on format
            if input.compression_format.lower() == "zip":
                compressed_data = await workflow.execute_activity(
                    compress_files_zip,
                    input,
                    start_to_close_timeout=timedelta(seconds=300),  # 5 minutes for large files
                )
            elif input.compression_format.lower() == "tar_gz":
                compressed_data = await workflow.execute_activity(
                    compress_files_tar_gz,
                    input,
                    start_to_close_timeout=timedelta(seconds=300),
                )
            else:
                raise ApplicationError(f"Unsupported compression format: {input.compression_format}")
            
            compressed_size = len(compressed_data)
            compression_ratio = ((original_size - compressed_size) / original_size * 100) if original_size > 0 else 0
            
            # Update progress - finalizing
            await workflow.execute_activity(
                update_job_progress,
                ProgressUpdate(
                    job_id=input.job_id,
                    progress=90,
                    status="finalizing",
                    message="Finalizing compression"
                ),
                start_to_close_timeout=timedelta(seconds=10),
            )
            
            # Encode compressed data as base64 for transfer
            compressed_data_b64 = base64.b64encode(compressed_data).decode()
            
            # Update final status
            await workflow.execute_activity(
                update_job_progress,
                ProgressUpdate(
                    job_id=input.job_id,
                    progress=100,
                    status="completed",
                    message="Compression completed successfully"
                ),
                start_to_close_timeout=timedelta(seconds=10),
            )
            
            return CompressionJobResult(
                job_id=input.job_id,
                compressed_data=compressed_data_b64,
                original_size=original_size,
                compressed_size=compressed_size,
                compression_ratio=compression_ratio,
                status="completed"
            )
            
        except Exception as e:
            workflow.logger.error(f"Compression workflow failed: {str(e)}")
            
            # Update error status
            await workflow.execute_activity(
                update_job_progress,
                ProgressUpdate(
                    job_id=input.job_id,
                    progress=0,
                    status="failed",
                    message=f"Compression failed: {str(e)}"
                ),
                start_to_close_timeout=timedelta(seconds=10),
            )
            
            raise ApplicationError(f"Compression workflow failed: {str(e)}")
