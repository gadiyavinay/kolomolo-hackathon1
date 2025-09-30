# 🗜️ Cloud File Compressor

**A complete cloud-based file compression platform built with modern microservices architecture**

## 🏆 Hackathon Project Overview

This is a full-stack cloud application that provides asynchronous file compression services with real-time progress tracking. Built for the Kolomolo Hackathon, it demonstrates enterprise-grade architecture using cutting-edge technologies.

## ✨ Features

### 🎯 Core Functionality
- **Multi-file compression** - Upload and compress multiple files simultaneously
- **Multiple formats** - Support for ZIP and TAR.GZ compression
- **Real-time progress tracking** - Live updates during compression process
- **Asynchronous processing** - Non-blocking workflow execution using Temporal
- **Cloud deployment** - Fully containerized with Polytope

### 🔧 Technical Features
- **Drag & Drop Interface** - Modern, intuitive file upload
- **RESTful API** - Clean, documented endpoints
- **Database Persistence** - PostgreSQL with job tracking
- **Workflow Orchestration** - Temporal workflows for reliability
- **Progress Monitoring** - Real-time status updates
- **File Download** - Direct download of compressed archives

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend UI   │    │   Backend API   │    │   Temporal      │
│                 │    │                 │    │   Workflows     │
│ React Router v7 │◄──►│ FastAPI +       │◄──►│                 │
│ shadcn/ui       │    │ SQLModel        │    │ File Compression│
│ TypeScript      │    │ Python          │    │ Progress Track  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   PostgreSQL    │
                    │   Database      │
                    │                 │
                    │ Job Tracking    │
                    │ File Metadata   │
                    └─────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- [Polytope](https://polytope.com/) - For container orchestration
- Node.js 18+
- Python 3.11+
- Git

### 1. Clone and Setup
```bash
git clone https://github.com/gadiyavinay/kolomolo-hackathon1.git
cd kolomolo-hackathon1
```

### 2. Deploy with Polytope
```bash
# Start all services
polytope run
```

This will automatically start:
- **Frontend**: http://localhost:51732
- **Backend API**: http://localhost:3030
- **Temporal UI**: http://localhost:8233
- **PostgreSQL**: localhost:5432

### 3. Access the Application
- **File Compressor UI**: http://localhost:51732
- **API Documentation**: http://localhost:3030/docs
- **Temporal Dashboard**: http://localhost:8233

## 🛠️ Technology Stack

### Frontend (Two Versions Available!)
- **🆕 React Router v7** (`modules/file-compressor-ui/`) - Modern routing and SSR with shadcn/ui
- **⭐ Angular 15** (`file-compressor/`) - Original implementation with working drag & drop
- **TypeScript** - Type-safe development across both apps
- **Tailwind CSS** - Utility-first styling
- **Modern Build Tools** - Bun (React) and Angular CLI

### Backend
- **FastAPI** - High-performance Python API framework
- **SQLModel** - Type-safe database ORM
- **Pydantic** - Data validation and serialization
- **uvicorn** - ASGI server

### Workflow Engine
- **Temporal** - Reliable workflow orchestration
- **Python SDK** - Durable execution guarantees
- **Progress Activities** - Real-time status updates

### Database
- **PostgreSQL** - Reliable relational database
- **Async SQLAlchemy** - Modern async ORM
- **Connection pooling** - High-performance database access

### Deployment
- **Polytope** - Container orchestration platform
- **Docker** - Containerization
- **Hot reload** - Development-friendly setup

## 📡 API Endpoints

### File Compression
```http
POST /compression/jobs
GET  /compression/jobs/{job_id}
GET  /compression/jobs/{job_id}/download
GET  /compression/jobs
POST /compression/upload
```

### Health & Status
```http
GET  /health
GET  /
```

### Example Usage
```bash
# Upload files for compression
curl -X POST "http://localhost:3030/compression/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "files=@file1.txt" \
  -F "files=@file2.txt" \
  -F "compression_format=zip"

# Check job status
curl "http://localhost:3030/compression/jobs/{job_id}"

# Download compressed file
curl "http://localhost:3030/compression/jobs/{job_id}/download" \
  -o compressed.zip
```

## 🔄 Workflow Architecture

The application uses **Temporal workflows** for reliable, asynchronous processing:

1. **Job Creation** - User uploads files via frontend
2. **Workflow Trigger** - API starts Temporal workflow
3. **Progress Updates** - Real-time database updates
4. **File Compression** - ZIP/TAR.GZ compression activities
5. **Result Storage** - Compressed data saved to database
6. **Download Ready** - User notified and can download

### Workflow Features
- **Fault tolerance** - Automatic retries on failure
- **Progress tracking** - Real-time status updates
- **Durability** - Workflows survive service restarts
- **Scalability** - Handle thousands of concurrent jobs

## 📊 Database Schema

```sql
-- Compression jobs table
CREATE TABLE compressionjob (
    id VARCHAR PRIMARY KEY,           -- UUID7
    workflow_id VARCHAR NOT NULL,     -- Temporal workflow ID
    status VARCHAR DEFAULT 'pending', -- Job status
    progress INTEGER DEFAULT 0,       -- 0-100%
    file_count INTEGER DEFAULT 0,
    original_size INTEGER DEFAULT 0,
    compressed_size INTEGER,
    compression_ratio FLOAT,
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    compressed_data TEXT              -- Base64 encoded result
);

-- Individual files table
CREATE TABLE compressionfile (
    id VARCHAR PRIMARY KEY,           -- UUID7
    job_id VARCHAR REFERENCES compressionjob(id),
    filename VARCHAR NOT NULL,
    size INTEGER NOT NULL,
    content TEXT NOT NULL,            -- Base64 encoded content
    created_at TIMESTAMP
);
```

## 🎨 Frontend Applications

This project includes **TWO complete frontend implementations** for maximum flexibility:

### ⭐ Angular 15 App (`/file-compressor/`)
**The working prototype with full drag & drop functionality**
- ✅ **Fully functional** file upload with drag & drop
- ✅ **File management** - add, remove, and clear files
- ✅ **Progress tracking** - real-time compression status
- ✅ **Working download** - tested and verified
- ✅ **Angular CLI** setup for easy development
- ✅ **TypeScript** with proper type safety
- 🚀 **Run with**: `cd file-compressor && ng serve`
- 🌐 **Access at**: http://localhost:4200

### 🆕 React Router v7 App (`/modules/file-compressor-ui/`)
**Modern cloud-native frontend with enterprise components**
- ✨ **shadcn/ui** components for professional design
- ✨ **React Router v7** with modern routing patterns
- ✨ **Tailwind CSS** utility-first styling
- ✨ **Bun runtime** for fast development
- ✨ **TypeScript** end-to-end type safety
- 🚀 **Run with**: `polytope run file-compressor-ui`
- 🌐 **Access at**: http://localhost:51732

### Common Features Across Both Apps
- **Drag & Drop Upload** - Intuitive file selection
- **Progress Bars** - Real-time compression progress
- **File Management** - Add/remove files before compression
- **Status Indicators** - Clear visual feedback
- **Download Interface** - One-click compressed file download
- **Responsive Design** - Works on all screen sizes
- **Accessible** - WCAG compliant interface

## 🔧 Development

### Local Development
```bash
# Start services individually
polytope run file-compressor-ui      # Frontend
polytope run file-compressor-api     # Backend
polytope run postgres                # Database
polytope run temporal                # Workflow engine
```

### Add Dependencies
```bash
# Frontend packages
polytope run file-compressor-ui-add --packages "axios react-query"

# Backend packages
polytope run file-compressor-api-add --packages "requests pillow"
```

### Database Management
```bash
# Connect to PostgreSQL
polytope run psql

# View database through web interface
polytope run pgweb
# Access at http://localhost:8081
```

### Monitor Workflows
- **Temporal UI**: http://localhost:8233
- View running workflows, history, and metrics
- Debug failed workflows
- Monitor system performance

## 🚦 Status Indicators

| Status | Description |
|--------|-------------|
| `pending` | Job created, waiting to start |
| `starting` | Workflow initializing |
| `preparing` | Files being prepared for compression |
| `compressing` | Active compression in progress |
| `finalizing` | Completing compression and storage |
| `completed` | Job finished successfully |
| `failed` | Job failed with error |

## 🏆 Hackathon Highlights

### Innovation
- **Full-stack cloud application** in record time
- **Enterprise architecture** with microservices
- **Real-time processing** with progress tracking
- **Modern tech stack** with latest frameworks

### Technical Excellence
- **Type-safe** end-to-end development
- **Async processing** for scalability  
- **Error handling** and retry logic
- **Database design** for performance
- **API documentation** with OpenAPI

### User Experience
- **Intuitive interface** with drag & drop
- **Real-time feedback** during processing
- **Mobile responsive** design
- **Accessible** to all users

## 🎯 Future Enhancements

- **Authentication & Authorization** - User accounts and permissions
- **File Format Support** - More compression formats (7z, RAR, etc.)
- **Cloud Storage** - Integration with S3, GCS, Azure Blob
- **Batch Processing** - Handle large file sets
- **Analytics Dashboard** - Usage metrics and insights
- **API Rate Limiting** - Prevent abuse
- **File Encryption** - Secure compressed archives

## 📖 Documentation

- **API Docs**: http://localhost:3030/docs (Swagger UI)
- **Temporal Dashboard**: http://localhost:8233
- **Database Schema**: See `modules/file-compressor-api/src/backend/db/models.py`
- **Workflow Code**: See `modules/file-compressor-api/src/backend/workflows/`

## 🤝 Team

Built with ❤️ for the Kolomolo Hackathon by the Cloud File Compressor team.

## 📄 License

MIT License - see LICENSE file for details.

---

**🎉 Ready to compress files in the cloud!** 🎉

Deploy with `polytope run` and visit http://localhost:51732 to start using the application.
