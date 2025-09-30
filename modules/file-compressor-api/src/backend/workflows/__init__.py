# Workflow and Activity Registry
# 
# Replace the example imports and registrations below with your own
# workflows and activities to register them with the Temporal client.
#
# Example:
# from .my_workflows import MyWorkflow, my_activity
# 
# Then add them to the WORKFLOWS and ACTIVITIES lists

from .examples import GreetingWorkflow, compose_greeting
from .file_compression import (
    FileCompressionWorkflow,
    update_job_progress,
    compress_files_zip,
    compress_files_tar_gz,
)

# Replace with your workflow classes
WORKFLOWS = [
    GreetingWorkflow,
    FileCompressionWorkflow,
]

# Replace with your activity functions
ACTIVITIES = [
    compose_greeting,
    update_job_progress,
    compress_files_zip,
    compress_files_tar_gz,
]
