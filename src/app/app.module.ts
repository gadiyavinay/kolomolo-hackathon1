import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { NgFileUploadModule } from 'ng-file-upload';
import { HttpClientModule } from '@angular/common/http';
import { CompressionService } from './compression.service';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    NgFileUploadModule,
    HttpClientModule
  ],
  providers: [
    CompressionService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
