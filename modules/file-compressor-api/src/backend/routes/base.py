import asyncio
import os
import sys
import time
from pathlib import Path
from typing import Optional, List
from fastapi import APIRouter, Request, HTTPException, Query

from ..utils import log
from .. import conf
# from ..utils import RequestPrincipal # NOTE: uncomment to use auth
from .utils import DBSession # NOTE: uncomment to use postgres

logger = log.get_logger(__name__)
router = APIRouter()

#### Utilities ####

def get_app_version() -> str:
    """Read version from pyproject.toml."""
    try:
        # Look for pyproject.toml from the current file up to project root
        current_path = Path(__file__).resolve()
        for parent in [current_path] + list(current_path.parents):
            pyproject_path = parent / "pyproject.toml"
            if pyproject_path.exists():
                content = pyproject_path.read_text()
                for line in content.split('\n'):
                    if line.strip().startswith('version = '):
                        # Extract version from 'version = "0.1.0"'
                        return line.split('=')[1].strip().strip('"\'')
                break
        return "unknown"
    except Exception as e:
        logger.warning(f"Failed to read version from pyproject.toml: {e}")
        return "unknown"

#### Routes ####

@router.get("/")
async def root():
    return {"message": "Hello World"}

@router.get("/health")
async def health_check(
    request: Request,
    quick: bool = Query(False, description="Return basic status only"),
    services: Optional[str] = Query(None, description="Comma-separated list of services to check (postgres,couchbase,temporal,twilio)"),
    timeout: float = Query(2.0, description="Timeout in seconds for health checks", ge=0.1, le=10.0)
):
    """Fast health check endpoint."""
    start_time = time.time()

    health_status = {
        "status": "healthy",
        "service": "backend",
        "timestamp": int(start_time),
    }

    # Add more extensive response if error surfacing is enabled
    if conf.get_http_expose_errors():
        health_status["dev_info"] = {
            "version": get_app_version(),
            "python_version": f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}",
            "features": {
                "postgres": conf.USE_POSTGRES,
                "couchbase": conf.USE_COUCHBASE,
                "temporal": conf.USE_TEMPORAL,
                "twilio": conf.USE_TWILIO,
                "auth": conf.USE_AUTH,
            },
            "configuration": {
                "log_level": conf.get_log_level(),
                "http_autoreload": conf.env.parse(conf.HTTP_AUTORELOAD),
            }
        }

    # Parse services filter
    services_to_check = None
    if services:
        services_to_check = [s.strip().lower() for s in services.split(",")]

    # Quick mode - just return basic status
    if quick:
        health_status["mode"] = "quick"
        health_status["response_time_ms"] = round((time.time() - start_time) * 1000, 2)
        return health_status

    await asyncio.wait_for(
        _check_all_services(request, health_status, services_to_check),
        timeout=timeout
    )

    # Add response time
    health_status["response_time_ms"] = round((time.time() - start_time) * 1000, 2)
    return health_status


async def _check_all_services(request: Request, health_status: dict, services_filter: Optional[List[str]]):
    """Check all enabled services with proper error handling."""

    # Check PostgreSQL if requested
    if not services_filter or "postgres" in services_filter:
        if conf.USE_POSTGRES:
            postgres_client = request.app.state.postgres_client
            db_health = postgres_client.health_check()
            health_status["postgres"] = db_health
            if not db_health.get("connected", False):
                health_status["status"] = "degraded"
        else:
            health_status["postgres"] = {
                "status": "disabled",
                "message": "PostgreSQL is disabled (USE_POSTGRES=False)"
            }

    # Check Couchbase if requested
    if not services_filter or "couchbase" in services_filter:
        if conf.USE_COUCHBASE:
            couchbase_client = request.app.state.couchbase_client
            couchbase_health = couchbase_client.health_check()
            health_status["couchbase"] = couchbase_health
            if not couchbase_health.get("connected", False):
                health_status["status"] = "degraded"
        else:
            health_status["couchbase"] = {
                "status": "disabled",
                "message": "Couchbase is disabled (USE_COUCHBASE=False)"
            }

    # Check Temporal if requested (with timeout protection)
    if not services_filter or "temporal" in services_filter:
        if conf.USE_TEMPORAL:
            temporal_client = request.app.state.temporal_client
            # Use health_check if available, otherwise use is_connected with timeout
            if hasattr(temporal_client, 'health_check'):
                temporal_health = temporal_client.health_check()
            else:
                # Wrap potentially blocking call in timeout
                try:
                    is_connected = await asyncio.wait_for(
                        asyncio.get_event_loop().run_in_executor(
                            None, temporal_client.is_connected
                        ),
                        timeout=0.5
                    )
                    temporal_health = {
                        "connected": is_connected,
                        "status": "connected" if is_connected else "disconnected"
                    }
                except asyncio.TimeoutError:
                    temporal_health = {
                        "connected": False,
                        "status": "timeout",
                        "message": "Connection check timed out"
                    }

            health_status["temporal"] = temporal_health
            if not temporal_health.get("connected", False):
                health_status["status"] = "degraded"
        else:
            health_status["temporal"] = {
                "status": "disabled",
                "message": "Temporal is disabled (USE_TEMPORAL=False)"
            }

    # Check Twilio if requested
    if not services_filter or "twilio" in services_filter:
        if conf.USE_TWILIO:
            twilio_client = request.app.state.twilio_client
            # Use health_check if available
            if hasattr(twilio_client, 'health_check'):
                twilio_health = twilio_client.health_check()
            else:
                twilio_health = {
                    "connected": True,
                    "status": "connected"
                }
            health_status["twilio"] = twilio_health
        else:
            health_status["twilio"] = {
                "status": "disabled",
                "message": "Twilio is disabled (USE_TWILIO=False)"
            }

    return health_status

# PostgreSQL route example using SQLModel (uncomment when using PostgreSQL)
#
# from .utils import DBSession
# from ..db.models import User, create_user, get_user, get_users
#
# @router.post("/users", response_model=User)
# async def create_user_route(user: User, session: DBSession):
#     """Create a new user."""
#     return await create_user(session, user)
#
# @router.get("/users/{user_id}", response_model=User)
# async def get_user_route(user_id: int, session: DBSession):
#     """Get a user by ID."""
#     user = await get_user(session, user_id)
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")
#     return user
#
# @router.get("/users", response_model=list[User])
# async def list_users_route(session: DBSession, skip: int = 0, limit: int = 100):
#     """List all users with pagination."""
#     return await get_users(session, skip=skip, limit=limit)


# Couchbase route example (uncomment when using Couchbase)
#
# from .utils import CouchbaseDB
# from ..clients.couchbase_models import CouchbaseUser, create_user, get_user, list_users
#
# @router.post("/cb/users", response_model=CouchbaseUser)
# async def create_user_cb(user: CouchbaseUser, cb: CouchbaseDB):
#     """Create a user in Couchbase."""
#     user_id = await create_user(cb, user)
#     user.id = user_id
#     return user
#
# @router.get("/cb/users/{user_id}", response_model=CouchbaseUser)
# async def get_user_cb(user_id: str, cb: CouchbaseDB):
#     """Get a user from Couchbase."""
#     user = await get_user(cb, user_id)
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")
#     return user
#
# @router.get("/cb/users", response_model=list[CouchbaseUser])
# async def list_users_cb(cb: CouchbaseDB, limit: int = 100, offset: int = 0):
#     """List users from Couchbase."""
#     return await list_users(cb, limit=limit, offset=offset)


# Temporal route examples (uncomment when using Temporal)
#
# import uuid
# from ..workflows.examples import GreetingWorkflow
#
# @router.post("/workflows/greeting")
# async def start_greeting_workflow(request: Request, name: str, greeting: str = "Hello"):
#     """Start a greeting workflow."""
#     temporal_client = request.app.state.temporal_client
#     workflow_id = f"greeting-{name}-{uuid.uuid4()}"
#
#     # IMPORTANT: For multiple workflow arguments, use args=[...]
#     handle = await temporal_client.start_workflow(
#         GreetingWorkflow.run,
#         args=[name, greeting],  # Multiple args must be passed as a list
#         id=workflow_id,
#         task_queue=temporal_client._config.task_queue,
#     )
#     return {"workflow_id": workflow_id, "message": f"Started workflow for {name}"}
#
# @router.get("/workflows/{workflow_id}/result")
# async def get_workflow_result(request: Request, workflow_id: str):
#     """Get the result of a workflow."""
#     temporal_client = request.app.state.temporal_client
#     try:
#         handle = temporal_client.get_workflow_handle(workflow_id)
#         result = await handle.result()
#
#         # Convert Pydantic models to dict for JSON serialization
#         if hasattr(result, 'model_dump'):
#             result = result.model_dump()
#
#         return {"workflow_id": workflow_id, "result": result, "status": "completed"}
#     except Exception as e:
#         return {"workflow_id": workflow_id, "error": str(e), "status": "error"}


# Twilio SMS route examples (uncomment when using Twilio)
#
# To enable Twilio SMS functionality:
# 1. Set USE_TWILIO = True in conf.py
# 2. Set environment variables:
#    - TWILIO_ACCOUNT_SID: Your Twilio Account SID
#    - TWILIO_AUTH_TOKEN: Your Twilio Auth Token  
#    - TWILIO_FROM_PHONE_NUMBER: Your Twilio phone number (e.g., '+15551234567')
# 3. Uncomment the routes below
#
# from pydantic import BaseModel
# from twilio.base.exceptions import TwilioRestException
#
# class SMSRequest(BaseModel):
#     to_phone_number: str
#     message: str
#
# @router.post("/sms/send")
# async def send_sms(request: Request, sms_request: SMSRequest):
#     """Send an SMS message via Twilio."""
#     if not conf.USE_TWILIO:
#         raise HTTPException(status_code=503, detail="Twilio SMS is disabled")
#     try:
#         twilio_client = request.app.state.twilio_client
#         result = await twilio_client.send_sms(
#             sms_request.to_phone_number,
#             sms_request.message
#         )
#         return {
#             "success": True,
#             "message_sid": result["sid"],
#             "status": result["status"],
#             "to": result["to"],
#             "message": "SMS sent successfully"
#         }
#     except TwilioRestException as e:
#         logger.error(f"Twilio error: {e}")
#         raise HTTPException(status_code=400, detail=f"Failed to send SMS: {e.msg}")
#     except Exception as e:
#         logger.error(f"Unexpected error sending SMS: {e}")
#         raise HTTPException(status_code=500, detail="Internal server error")
#
# # Example: Send SMS with Temporal workflow for delayed/scheduled messages
# @router.post("/sms/send-delayed")
# async def send_delayed_sms(request: Request, sms_request: SMSRequest, delay_minutes: int = 5):
#     """Send a delayed SMS message using Temporal workflow."""
#     if not conf.USE_TWILIO:
#         raise HTTPException(status_code=503, detail="Twilio SMS is disabled")
#     if not conf.USE_TEMPORAL:
#         raise HTTPException(status_code=503, detail="Temporal is disabled")
#
#     # This would require implementing a Temporal workflow for SMS
#     # Example workflow implementation would go in clients/temporal.py:
#     #
#     # @workflow.defn
#     # class DelayedSMSWorkflow:
#     #     @workflow.run
#     #     async def run(self, phone_number: str, message: str, delay_minutes: int) -> dict:
#     #         await asyncio.sleep(delay_minutes * 60)
#     #         return await workflow.execute_activity(
#     #             send_sms_activity,
#     #             args=[phone_number, message],
#     #             start_to_close_timeout=timedelta(minutes=1)
#     #         )
#
#     temporal_client = request.app.state.temporal_client
#     workflow_id = f"delayed-sms-{uuid.uuid4()}"
#
#     # Start workflow (implementation would depend on your Temporal setup)
#     # handle = await temporal_client.client.start_workflow(
#     #     DelayedSMSWorkflow.run,
#     #     args=[sms_request.to_phone_number, sms_request.message, delay_minutes],
#     #     id=workflow_id,
#     #     task_queue=temporal_client.config.task_queue
#     # )
#
#     return {
#         "workflow_id": workflow_id,
#         "message": f"Delayed SMS scheduled for {delay_minutes} minutes",
#         "to": sms_request.to_phone_number
#     }
#

# File Compression API Routes

import uuid
import base64
from pydantic import BaseModel
from typing import List
from fastapi import UploadFile, File, Form
from ..db.models import (
    CompressionJob, CompressionFile,
    create_compression_job, get_compression_job, 
    add_files_to_job, list_compression_jobs,
    update_compression_job_progress
)
from ..workflows.file_compression import (
    FileCompressionWorkflow,
    CompressionJobInput,
    FileItem
)


class CompressionJobRequest(BaseModel):
    """Request model for creating a compression job"""
    files: List[dict]  # [{"name": "file.txt", "content": "base64...", "size": 123}]
    compression_format: str = "zip"
    compression_level: int = 6


class CompressionJobResponse(BaseModel):
    """Response model for compression job"""
    job_id: str
    workflow_id: str
    status: str
    progress: int
    message: Optional[str] = None
    file_count: int
    original_size: int
    compressed_size: Optional[int] = None
    compression_ratio: Optional[float] = None
    created_at: str
    started_at: Optional[str] = None
    completed_at: Optional[str] = None


@router.post("/compression/jobs", response_model=CompressionJobResponse)
async def create_compression_job_route(
    request: Request,
    job_request: CompressionJobRequest,
    session: DBSession
):
    """
    Create a new file compression job and start the Temporal workflow.
    
    Files should be provided as base64-encoded content in the request.
    """
    if not conf.USE_TEMPORAL:
        raise HTTPException(status_code=503, detail="Temporal workflows are disabled")
    
    if not job_request.files:
        raise HTTPException(status_code=400, detail="No files provided")
    
    # Generate unique job ID
    job_id = str(uuid.uuid4())
    workflow_id = f"compression-{job_id}"
    
    # Calculate total size
    total_size = sum(file["size"] for file in job_request.files)
    
    # Create database record
    compression_job = CompressionJob(
        id=job_id,
        workflow_id=workflow_id,
        status="pending",
        file_count=len(job_request.files),
        original_size=total_size,
        compression_format=job_request.compression_format
    )
    
    job = await create_compression_job(session, compression_job)
    
    # Add files to database
    await add_files_to_job(session, job_id, job_request.files)
    
    # Prepare Temporal workflow input
    file_items = [
        FileItem(
            name=file["name"],
            content=file["content"],
            size=file["size"]
        )
        for file in job_request.files
    ]
    
    workflow_input = CompressionJobInput(
        job_id=job_id,
        files=file_items,
        compression_format=job_request.compression_format,
        compression_level=job_request.compression_level
    )
    
    # Start Temporal workflow
    temporal_client = request.app.state.temporal_client
    try:
        handle = await temporal_client.start_workflow(
            FileCompressionWorkflow.run,
            workflow_input,
            id=workflow_id,
            task_queue=temporal_client._config.task_queue,
        )
        
        logger.info(f"Started compression workflow {workflow_id} for job {job_id}")
        
    except Exception as e:
        logger.error(f"Failed to start workflow: {e}")
        # Update job status to failed
        await update_compression_job_progress(
            session, job_id, 0, "failed", f"Failed to start workflow: {str(e)}"
        )
        raise HTTPException(status_code=500, detail=f"Failed to start compression workflow: {str(e)}")
    
    return CompressionJobResponse(
        job_id=job.id,
        workflow_id=job.workflow_id,
        status=job.status,
        progress=job.progress,
        message=job.message,
        file_count=job.file_count,
        original_size=job.original_size,
        compressed_size=job.compressed_size,
        compression_ratio=job.compression_ratio,
        created_at=job.created_at.isoformat(),
        started_at=job.started_at.isoformat() if job.started_at else None,
        completed_at=job.completed_at.isoformat() if job.completed_at else None,
    )


@router.get("/compression/jobs/{job_id}", response_model=CompressionJobResponse)
async def get_compression_job_route(job_id: str, session: DBSession):
    """Get the status and details of a compression job."""
    job = await get_compression_job(session, job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Compression job not found")
    
    return CompressionJobResponse(
        job_id=job.id,
        workflow_id=job.workflow_id,
        status=job.status,
        progress=job.progress,
        message=job.message,
        file_count=job.file_count,
        original_size=job.original_size,
        compressed_size=job.compressed_size,
        compression_ratio=job.compression_ratio,
        created_at=job.created_at.isoformat(),
        started_at=job.started_at.isoformat() if job.started_at else None,
        completed_at=job.completed_at.isoformat() if job.completed_at else None,
    )


@router.get("/compression/jobs/{job_id}/download")
async def download_compressed_file(job_id: str, session: DBSession):
    """Download the compressed file for a completed job."""
    job = await get_compression_job(session, job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Compression job not found")
    
    if job.status != "completed":
        raise HTTPException(status_code=400, detail=f"Job is not completed. Current status: {job.status}")
    
    if not job.compressed_data:
        raise HTTPException(status_code=404, detail="Compressed data not available")
    
    # Decode base64 compressed data
    try:
        compressed_bytes = base64.b64decode(job.compressed_data)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to decode compressed data: {str(e)}")
    
    # Determine file extension based on format
    file_extension = "zip" if job.compression_format == "zip" else "tar.gz"
    filename = f"compressed-{job_id}.{file_extension}"
    
    from fastapi.responses import Response
    
    return Response(
        content=compressed_bytes,
        media_type="application/octet-stream",
        headers={
            "Content-Disposition": f"attachment; filename={filename}",
            "Content-Length": str(len(compressed_bytes)),
        }
    )


@router.get("/compression/jobs", response_model=List[CompressionJobResponse])
async def list_compression_jobs_route(session: DBSession, limit: int = Query(50, le=100)):
    """List recent compression jobs."""
    jobs = await list_compression_jobs(session, limit=limit)
    
    return [
        CompressionJobResponse(
            job_id=job.id,
            workflow_id=job.workflow_id,
            status=job.status,
            progress=job.progress,
            message=job.message,
            file_count=job.file_count,
            original_size=job.original_size,
            compressed_size=job.compressed_size,
            compression_ratio=job.compression_ratio,
            created_at=job.created_at.isoformat(),
            started_at=job.started_at.isoformat() if job.started_at else None,
            completed_at=job.completed_at.isoformat() if job.completed_at else None,
        )
        for job in jobs
    ]


@router.post("/compression/upload")
async def upload_and_compress_files(
    request: Request,
    session: DBSession,
    files: List[UploadFile] = File(...),
    compression_format: str = Form("zip"),
    compression_level: int = Form(6)
):
    """
    Upload multiple files and create a compression job.
    
    This endpoint accepts multipart form data with files.
    """
    if not files:
        raise HTTPException(status_code=400, detail="No files uploaded")
    
    # Process uploaded files
    file_data = []
    for uploaded_file in files:
        try:
            # Read file content
            content = await uploaded_file.read()
            
            # Encode as base64
            content_b64 = base64.b64encode(content).decode()
            
            file_data.append({
                "name": uploaded_file.filename,
                "content": content_b64,
                "size": len(content)
            })
            
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to process file {uploaded_file.filename}: {str(e)}")
    
    # Create compression job request
    job_request = CompressionJobRequest(
        files=file_data,
        compression_format=compression_format,
        compression_level=compression_level
    )
    
    # Use the existing create job endpoint
    return await create_compression_job_route(request, job_request, session)
