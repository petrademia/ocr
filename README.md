# Java File Upload to Google Drive with OCR using Tesseract

This is a Java project that demonstrates how to upload a file to Google Drive and perform OCR using Tesseract. The project uses the Google Drive API to upload the file and the Tesseract OCR library to perform optical character recognition on the uploaded file.

## Prerequisites

Before running this project, you will need:

- A Google account with Google Drive API access enabled
- Java JDK installed on your system
- Gradle build tool installed on your system
- Tesseract OCR library installed on your system

## Getting Started

1. Clone the repository to your local machine:

git clone https://github.com/petrademia/ocr.git

2.

- OcrController contains the handler for accepting the 

Notes:

It would be good if we are not enforcing the necessity of using Java for such an image processing case,
because there are multiple available image processing library in Python/Go.
Unit test can be added to ensure that the endpoint is working perfectly without manual testing.
Credentials and/or sensitive information can be stored in a third party secret store like Vault.
