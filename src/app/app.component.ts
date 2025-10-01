import { Component } from '@angular/core';
import { CompressionService } from './compression.service';
import { HttpClient } from '@angular/common/http';
import { saveAs } from 'file-saver';

const level: unique symbol = Symbol();

interface MyInterface {
  [level]?: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'file-compressor';
  uploadedFiles: File[] = [];

  constructor(
    private compressionService: CompressionService,
    private http: HttpClient
  ) {}

  onFileUpload(files: File[]) {
    this.uploadedFiles = files;
  }

  downloadCompressedFile() {
    this.compressionService.compressFiles(this.uploadedFiles)
      .then((compressedFile: Blob) => {
        saveAs(compressedFile, 'compressed_files.zip');
      });
  }
}
