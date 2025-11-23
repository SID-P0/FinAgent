![ProposedDesign drawio](https://github.com/user-attachments/assets/27044cfe-f769-4610-bd1d-b67f87c48a7c)

Steps to run locally

1. Add in vm options
   -Djava.awt.headless=false

2. Create a secrets.properties and add your gemini key 
   API_KEY = .........................

3. Add tessaract datapath as well as install the tessaract based on your system configurations
   tesseract.datapath = C:/Program Files/Tesseract-OCR/tessdata (Depends where you installed the binaries)
