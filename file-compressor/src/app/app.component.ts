import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'file-compressor';
  
  selectedFiles: File[] = [];
  hasFileOver = false;
  isCompressing = false;
  compressionProgress = 0;
  compressedData: Blob | null = null;
  originalSize = 0;
  compressedSize = 0;
  compressionRatio = 0;

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.selectedFiles = Array.from(input.files);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.hasFileOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.hasFileOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.hasFileOver = false;
    
    if (event.dataTransfer?.files) {
      this.selectedFiles = Array.from(event.dataTransfer.files);
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  clearFiles(): void {
    this.selectedFiles = [];
    this.compressedData = null;
  }

  addTestFiles(): void {
    // Create some test files for demonstration
    const testFile1 = new File(['This is test file 1 content. Hello world!'], 'test1.txt', { type: 'text/plain' });
    const testFile2 = new File(['This is test file 2 with more content for testing compression functionality.'], 'test2.txt', { type: 'text/plain' });
    const testFile3 = new File(['{"name": "test", "value": 123, "items": ["a", "b", "c"]}'], 'data.json', { type: 'application/json' });
    
    this.selectedFiles = [testFile1, testFile2, testFile3];
  }

  async compressFiles(): Promise<void> {
    if (this.selectedFiles.length === 0) {
      return;
    }

    this.isCompressing = true;
    this.compressionProgress = 0;
    this.compressedData = null;

    // Calculate original size
    this.originalSize = this.selectedFiles.reduce((sum, file) => sum + file.size, 0);

    try {
      // Simple compression simulation - in a real app you'd use a proper compression library
      this.compressionProgress = 25;
      await this.delay(500);
      
      this.compressionProgress = 50;
      await this.delay(500);
      
      this.compressionProgress = 75;
      await this.delay(500);
      
      // Create a simple "compressed" file by combining all files into a single blob
      const fileContents: string[] = [];
      
      for (let i = 0; i < this.selectedFiles.length; i++) {
        const file = this.selectedFiles[i];
        const content = await this.readFileAsText(file);
        fileContents.push(`--- ${file.name} ---\n${content}\n\n`);
      }
      
      const combinedContent = fileContents.join('');
      this.compressedData = new Blob([combinedContent], { type: 'text/plain' });
      this.compressedSize = this.compressedData.size;
      
      // Calculate compression ratio (this is just simulated)
      this.compressionRatio = Math.max(0, Math.round(((this.originalSize - this.compressedSize) / this.originalSize) * 100));
      
      this.compressionProgress = 100;
    } catch (error) {
      console.error('Compression failed:', error);
    } finally {
      this.isCompressing = false;
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          resolve(reader.result as string);
        } catch {
          resolve(`[Binary file: ${file.name}]`);
        }
      };
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }

  downloadCompressed(): void {
    if (!this.compressedData) {
      return;
    }

    const url = window.URL.createObjectURL(this.compressedData);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'compressed-files.txt';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
