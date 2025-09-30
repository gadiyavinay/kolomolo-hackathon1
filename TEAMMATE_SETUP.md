# 🚀 Teammate Setup Guide - Kolomolo Hackathon

**Quick setup guide for team members to get the file compression platform running**

## ✅ Prerequisites

Make sure you have these installed:
- **Polytope** - Container orchestration platform
- **Git** - Version control
- **Node.js 18+** - For Angular app (optional)

## 🎯 Quick Start (2 minutes)

### 1. Clone the Repository
```bash
git clone https://github.com/gadiyavinay/kolomolo-hackathon1.git
cd kolomolo-hackathon1
```

### 2. Deploy Everything with One Command
```bash
polytope run
```

**That's it!** ✨ All services will start automatically.

## 🌐 Access the Applications

After `polytope run` completes, you can access:

- **🎨 Modern React UI**: http://localhost:51732
- **⚡ Backend API**: http://localhost:3030
- **📊 API Documentation**: http://localhost:3030/docs
- **🔄 Temporal Dashboard**: http://localhost:8233
- **💾 Database Web UI**: http://localhost:8081 (run `polytope run pgweb`)

### Alternative: Angular App
If you prefer the Angular version:
```bash
cd file-compressor
npm install
ng serve
# Access at http://localhost:4200
```

## 🧪 Test the Platform

### Test 1: API Health Check
```bash
curl http://localhost:3030/health | jq
```
Should return `"status": "healthy"` with all services connected.

### Test 2: File Compression
```bash
# Create test files
echo "Hello World" > test.txt
echo "Another file" > test2.txt

# Upload and compress
curl -X POST "http://localhost:3030/compression/upload" \
  -F "files=@test.txt" \
  -F "files=@test2.txt" \
  -F "compression_format=zip"

# Check the job status (use job_id from above response)
curl "http://localhost:3030/compression/jobs/{JOB_ID}"

# Download compressed file
curl "http://localhost:3030/compression/jobs/{JOB_ID}/download" -o compressed.zip
```

### Test 3: Frontend
1. Visit http://localhost:51732
2. Drag and drop files
3. Click "Compress Files"
4. Monitor progress
5. Download result

## 🐛 Troubleshooting

### Service Not Starting?
```bash
# Check running services
polytope list-services

# Check logs
polytope get-container-logs {container-name}

# Restart all
polytope stop
polytope run
```

### API Errors?
- Wait 30 seconds for all services to fully initialize
- Check http://localhost:3030/health
- PostgreSQL and Temporal must show "connected": true

### Frontend Issues?
- Ensure API is running on port 3030
- Check browser console for errors
- Try the Angular version at http://localhost:4200

## 🏗️ Architecture Overview

```
Frontend (React/Angular) → API (FastAPI) → Temporal Workflows → PostgreSQL
                            ↓
                        Progress Tracking
```

## 📁 Project Structure

```
kolomolo-hackathon1/
├── modules/
│   ├── file-compressor-ui/     # React Router v7 app
│   ├── file-compressor-api/    # FastAPI backend
│   ├── postgres/               # Database
│   └── temporal/               # Workflow engine
├── file-compressor/            # Angular 15 app
├── polytope.yml                # Deployment config
└── README.md                   # Full documentation
```

## 🎯 Demo Tips

### For Presentations:
1. **Start with health check** - Show all services running
2. **Upload multiple files** - Demonstrate the drag & drop
3. **Show real-time progress** - Temporal dashboard at :8233
4. **Download and verify** - Prove compression works
5. **Show the code** - Highlight Temporal workflows

### Key Features to Highlight:
- ✅ **Microservices architecture** with Docker containers
- ✅ **Asynchronous processing** with Temporal workflows
- ✅ **Real-time progress tracking** with WebSockets
- ✅ **Database persistence** with PostgreSQL
- ✅ **Multiple compression formats** (ZIP, TAR.GZ)
- ✅ **Two frontend options** (React + Angular)
- ✅ **Enterprise-grade** deployment with Polytope

## 🚀 Development Commands

```bash
# Individual service management
polytope run file-compressor-ui      # Frontend only
polytope run file-compressor-api     # Backend only
polytope run postgres                # Database only
polytope run temporal                # Workflows only

# Add packages
polytope run file-compressor-ui-add --packages "axios"
polytope run file-compressor-api-add --packages "pillow"

# Database access
polytope run psql                    # PostgreSQL CLI
polytope run pgweb                   # Web interface
```

## 🎉 Success Indicators

✅ **All services healthy** - http://localhost:3030/health  
✅ **Frontend loads** - http://localhost:51732  
✅ **File upload works** - Drag & drop interface  
✅ **Compression completes** - Job status becomes "completed"  
✅ **Download works** - ZIP file downloads successfully  

## 💡 Pro Tips

- **Use multiple terminal tabs** - Monitor logs while testing
- **Check Temporal UI** - Watch workflows execute in real-time
- **Test with various file types** - Show platform flexibility
- **Highlight the architecture** - This is enterprise-grade stuff!

---

**🎯 Ready to impress the judges!** Your hackathon platform is production-ready with enterprise architecture, real-time processing, and beautiful UIs.

**Need help?** Check the main README.md for detailed documentation.
