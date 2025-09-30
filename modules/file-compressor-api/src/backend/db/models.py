"""
Database models using SQLModel.

IMPORTANT: All SQLModel table classes defined in this file are automatically
registered when this module is imported. The app's lifespan function in main.py
imports this module and creates the tables during startup.

To add a new model:
1. Define your class with SQLModel and table=True
2. The table will be automatically created on next startup
3. Add any necessary database functions below the model definitions
"""

from sqlmodel import SQLModel, Field, select
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional, List
from datetime import datetime
from ..db.utils import pk_field


class CompressionJob(SQLModel, table=True):
    """Database model for compression jobs"""
    id: str = pk_field()
    workflow_id: str = Field(index=True)  # Temporal workflow ID
    status: str = Field(default="pending")  # pending, starting, preparing, compressing, finalizing, completed, failed
    progress: int = Field(default=0)  # 0-100
    message: Optional[str] = Field(default=None)
    
    # File information
    file_count: int = Field(default=0)
    original_size: int = Field(default=0)
    compressed_size: Optional[int] = Field(default=None)
    compression_ratio: Optional[float] = Field(default=None)
    compression_format: str = Field(default="zip")  # zip, tar_gz
    
    # Timestamps
    created_at: datetime = Field(default_factory=datetime.utcnow)
    started_at: Optional[datetime] = Field(default=None)
    completed_at: Optional[datetime] = Field(default=None)
    
    # Result data
    compressed_data: Optional[str] = Field(default=None)  # Base64 encoded compressed data


class CompressionFile(SQLModel, table=True):
    """Database model for individual files in a compression job"""
    id: str = pk_field()
    job_id: str = Field(foreign_key="compressionjob.id", index=True)
    filename: str
    size: int
    content: str  # Base64 encoded file content
    created_at: datetime = Field(default_factory=datetime.utcnow)


# Database functions for compression jobs

async def create_compression_job(session: AsyncSession, job: CompressionJob) -> CompressionJob:
    """Create a new compression job"""
    session.add(job)
    await session.flush()  # Get the ID without committing
    return job


async def get_compression_job(session: AsyncSession, job_id: str) -> Optional[CompressionJob]:
    """Get compression job by ID"""
    result = await session.execute(select(CompressionJob).where(CompressionJob.id == job_id))
    return result.scalar_one_or_none()


async def update_compression_job_progress(
    session: AsyncSession, 
    job_id: str, 
    progress: int, 
    status: str, 
    message: Optional[str] = None
) -> Optional[CompressionJob]:
    """Update compression job progress"""
    result = await session.execute(select(CompressionJob).where(CompressionJob.id == job_id))
    job = result.scalar_one_or_none()
    
    if job:
        job.progress = progress
        job.status = status
        if message:
            job.message = message
        
        # Set timestamps based on status
        if status == "starting" and not job.started_at:
            job.started_at = datetime.utcnow()
        elif status in ["completed", "failed"]:
            job.completed_at = datetime.utcnow()
        
        session.add(job)
        await session.flush()
    
    return job


async def complete_compression_job(
    session: AsyncSession,
    job_id: str,
    compressed_size: int,
    compression_ratio: float,
    compressed_data: str
) -> Optional[CompressionJob]:
    """Complete a compression job with results"""
    result = await session.execute(select(CompressionJob).where(CompressionJob.id == job_id))
    job = result.scalar_one_or_none()
    
    if job:
        job.status = "completed"
        job.progress = 100
        job.compressed_size = compressed_size
        job.compression_ratio = compression_ratio
        job.compressed_data = compressed_data
        job.completed_at = datetime.utcnow()
        job.message = "Compression completed successfully"
        
        session.add(job)
        await session.flush()
    
    return job


async def add_files_to_job(session: AsyncSession, job_id: str, files: List[dict]) -> List[CompressionFile]:
    """Add files to a compression job"""
    compression_files = []
    
    for file_data in files:
        comp_file = CompressionFile(
            job_id=job_id,
            filename=file_data["name"],
            size=file_data["size"],
            content=file_data["content"]
        )
        session.add(comp_file)
        compression_files.append(comp_file)
    
    await session.flush()
    return compression_files


async def get_job_files(session: AsyncSession, job_id: str) -> List[CompressionFile]:
    """Get all files for a compression job"""
    result = await session.execute(select(CompressionFile).where(CompressionFile.job_id == job_id))
    return result.scalars().all()


async def list_compression_jobs(session: AsyncSession, limit: int = 50) -> List[CompressionJob]:
    """List recent compression jobs"""
    result = await session.execute(
        select(CompressionJob)
        .order_by(CompressionJob.created_at.desc())
        .limit(limit)
    )
    return result.scalars().all()
