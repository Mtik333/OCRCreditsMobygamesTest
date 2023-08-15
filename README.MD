# OCRCreditsMobygamesTest

As name suggests, it's more about trying to have some tool that will read the text from the screenshots provided into it. Depending on the case, it should result in a text that is compatible with "import credits" tool available at Mobygames.

Using Tesseract for OCRing the screenshots and OpenNLP for "trying-to-detect" names and non-names.

## How to deploy

Well there is a Dockerfile and it provides all the commands necessary to be done in Linux environment. I believe it is also sufficient to just import this stuff via Java IDE like IntelliJ and it should be able to run this thing. 

## Example of usage

Check this example for PGA 2K23
[![OCR Credits Mobygames Tool PGA 2K23](https://img.youtube.com/vi/CMHLKLidumI/0.jpg)](https://www.youtube.com/watch?v=CMHLKLidumI)

Also this example for F1 Manager 2022
[![OCR Credits MobyGames Tool F1 Manager 2022](https://img.youtube.com/vi/PD7dfzLSFww/0.jpg)](https://www.youtube.com/watch?v=PD7dfzLSFww)


## Settings

### OCR checkboxes
There is a bunch of checkboxes to tweak behaviour of the tool so let's briefly review that.

- "Dark background and white font" - when the screenshots show text in white font on a dark background (does not have to be black), Tesseract might have problems with OCRing it properly. Therefore tool will invert and grayscale the image (so the background becomes light and font - dark or basically black). 
- "Try to group/fix output automatically" - if you want to prepare this text directly for "import credits" module at Mobygames, then use it - still you might need to re-check the output before copying it all
- "Replace comma with new line" - sometimes developers are listed using commas and not new lines, and for the usage of import tool we need to list them line by line, so use this when you know that it is the case in your credits source
- "Colors of role / dev letters have low contrast" - sometimes role titles and devs names are displayed with different color - see [f1manager22.jpeg](examples/f1manager22.jpeg) - in such case grayscaling would cause this red font to be too "light" to be redable via Tesseract. So basically tick it when dev font and role font are of similar color
- "Do not use OpenNLP to perform name recognition and assume each dev name consists of 2 words" - this is used only for cases when dev names are displayed next to the roles. OpenNLP tool used to detect if text contains name is unable to work properly as I do not have data to train it and make it work, so in this case we just assume that 2 last words in the line will be devs names and so need to be moved to a new line
- "Try to correct nicknames" - as you might know, we have to escape nicknames when typing such dev into the Mobygames credits: you should not do "Gabe 'GabeN' Newell", but rather "Gabe Newell ('GabeN')". Tick this to make the tool try to do it automatically
- "Try to correct UPPERCASE DEV NAMES" - as it says, tool tries to change "JOHN SMITH" to "John Smith" - this applies only to dev names

### OCR dropdowns
- "Resolution" dropdown - change it to modify display of the source images on the page (might be useful when you got 1080p screenshots and it takes a lot of space)
- "Dev display" - most common are of devs being below the role title and devs names being displayed in the same line as the role title. Choose based on your input
- Segmentation mode - see [this article](https://pyimagesearch.com/2021/11/15/tesseract-page-segmentation-modes-psms-explained-how-to-improve-your-ocr-accuracy/)
- Engine mode - see [this article](https://ai-facets.org/tesseract-ocr-best-practices/)

### OCR buttons
- "OCR all images" - will go through all files and OCR them, and give output for each image
- "Show combined text" - shows the whole text fetched from images "together" on the page
- "Hide combined text" - hides this section with whole text from images
- "Copy combined text" - copied the whole text to clipboard
- "Clear text for all images" - if you used wrong settings and you want to repeat OCRing with different ones, use it
- "Delete all images" - deletes everything you uploaded so you can go with another set of images


## How to start

I assume here that you already started the app - it should be accessible at [http://localhost:8080/greeting](http://localhost:8080/greeting).

Let's prepare example screenshot - [f1manager22.jpeg](examples/f1manager22.jpeg). As you can see, we have format where there is a role and developers in such role below this role title. Upload the image and wait for dialog to close. Check the settings: as screenshot is dark, we should use the checkbox related to it. We also want to group output automatically. Role and dev titles have different colors so we keep this checkbox not ticked. Finally, we can try to correct nicknames. Then hit the "OCR all images" and wait for the result

Let's prepare another example screenshot - [pga2k23.jpeg](examples/pga2k23.jpeg). As you can see, we have the situation that role is on the left side and dev names - on the right side. Therefore, compared to first example, we have to modify the dropdown and use "dev names next to (right) role title". Also, we don't want to OCR the text on the left, so you can push left mouse key down on the image and create rectangle that should be used for OCRing. Hit the "OCR all images" and check the result.