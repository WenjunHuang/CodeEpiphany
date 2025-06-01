import 'normalize-css/normalize.css';
import {Marked} from 'marked'
import {markedHighlight} from "marked-highlight";
import hljs from 'highlight.js';
import 'highlight.js/styles/vs.css';
import 'katex/dist/katex.min.css';
import renderMathInElement from 'katex/contrib/auto-render';
// @ts-ignore

declare global {
    interface Window {
        showSolutionArticle: (content: string, getIframeUrl: string) => void;
    }
}

function showSolutionArticle(content: string, getIframeUrl: string) {
    const container = document.getElementById('container')!;
    const marked = new Marked(markedHighlight({
        emptyLangClass: 'hljs',
        langPrefix: 'hljs language-',
        highlight(code, lang) {
            const language = hljs.getLanguage(lang) ? lang : 'plaintext';
            console.log(lang, language);
            return hljs.highlight(code, {language}).value;
        }
    }));
    container.innerHTML = <string>marked.parse(content);
    renderMathInElement(document.body, {
        delimiters: [
            {left: '$$', right: '$$', display: false},
            {left: '$', right: '$', display: false},
            {left: '\\(', right: '\\)', display: false},
            {left: '\\[', right: '\\]', display: true}
        ],
        throwOnError: false
    });

    document.querySelectorAll('iframe').forEach((iframe) => {
        const src = iframe.src;
        console.log(src);
        if (src.includes('leetcode.com')) {
            const original = iframe.src;
            iframe.src = getIframeUrl + "?url=" + encodeURI(original);
        }
    });

}

export function initialize() {
    window.showSolutionArticle = showSolutionArticle;
}
