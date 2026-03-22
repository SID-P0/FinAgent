![ProposedDesign drawio](https://github.com/user-attachments/assets/27044cfe-f769-4610-bd1d-b67f87c48a7c)

Steps to run locally

1. Add in vm options
   -Djava.awt.headless=false

2. Create a secrets.properties and add your gemini key 
   API_KEY = .........................

3. Add tessaract datapath as well as install the tessaract based on your system configurations
   tesseract.datapath = C:/Program Files/Tesseract-OCR/tessdata (Depends where you installed the binaries)

4. Try prompts like "Go to incognito chrome search for bajaj stock then click on 'News' section then click on all links","Open chrome search for wordle".

5. Go to incognito chrome search for bajaj stock then click on 'News' section then click on all links then scroll below little bit and again click on all links repeat this 3/4 times


<thought>
First, I need to open an incognito tab in Chrome. Then navigate to a search query related to "bajaj stock". Find the news section and click it. Click on all blue links visible on that page. Scroll down and then click on any new blue links visible. Repeat this process two more times.
</thought>
1. openNewTab
2. navigateToUrl: https://www.google.com/
3. searchInChrome: bajaj stock
4. findAndClickText: News
5. clickAllBlueLinks
6. scrollPercentage: 100
7. clickAllBlueLinks
8. scrollPercentage: 100
9. clickAllBlueLinks
10. scrollPercentage: 100