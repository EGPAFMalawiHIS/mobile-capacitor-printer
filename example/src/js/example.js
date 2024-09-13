import { HtmlToPdfSaver } from 'html-to-pdf-saver';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    HtmlToPdfSaver.echo({ value: inputValue })
}
