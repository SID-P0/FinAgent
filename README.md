Features to add
1. Integrate LLM with robot class to handle the mouse actions
2. Make the LLM decide the important text from news articles
3. Caching and database hits
4. Pass it to FinBert for sentiment analysis
5. UI to display information



Steps to run locally

1. Add in vm options
   -Djava.awt.headless=false

2. Create a secrets.properties and add your gemini key 
   API_KEY = .........................

3. Add tessaract datapath as well as install the tessaract based on your system configurations
   tesseract.datapath = C:/Program Files/Tesseract-OCR/tessdata (Depends where you installed the binaries)