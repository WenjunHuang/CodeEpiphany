import {Marked} from 'marked'
import {markedHighlight} from "marked-highlight";
import hljs from 'highlight.js';
import './app.scss';
import 'katex/dist/katex.css';
import renderMathInElement from 'katex/contrib/auto-render';
import katex_extension from "./katex_extension";

declare global {
    interface Window {
        showSolutionArticle: (content: string,
                              codeDojo: string,
                              getIframeUrl: string) => void;
    }
}

const showSolutionArticle = (contentBased64: string,
                             codeDojoBased64: string,
                             getIframeUrlBased64: string) => {
    const content = decodeBase64(contentBased64);
    const codeDojo = decodeBase64(codeDojoBased64);
    // @ts-ignore
    const getIframeUrl = decodeBase64(getIframeUrlBased64);
    const container = document.getElementById('container')!;
    let marked = new Marked(markedHighlight({
        emptyLangClass: 'hljs',
        langPrefix: 'hljs language-',
        highlight(code, lang) {
            const language = hljs.getLanguage(lang) ? lang : 'plaintext';
            return hljs.highlight(code, {language}).value;
        }
    }));
    if (codeDojo.toLowerCase() === 'leetcodecn')
        marked = marked.use(katex_extension({}));


    container.innerHTML = <string>marked.parse(content);
    if (codeDojo.toLowerCase() === 'leetcode') {
        renderMathInElement(container, {
            delimiters: [
                {left: '$$', right: '$$', display: false},
                {left: '$', right: '$', display: false},
                {left: '\\(', right: '\\)', display: false},
                {left: '\\[', right: '\\]', display: true}
            ],
            throwOnError: true
        });

        // document.querySelectorAll('iframe').forEach((iframe) => {
        //     const src = iframe.src;
        //     console.log(src);
        //     if (src.includes('leetcode.com')) {
        //         const original = iframe.src;
        //         iframe.src = getIframeUrl + "?url=" + encodeURI(original);
        //     }
        // });
    }

}

function decodeBase64(base64Str: string): string {
    const binaryStr = atob(base64Str);
    const bytes = new Uint8Array(binaryStr.length);
    for (let i = 0; i < binaryStr.length; i++) {
        bytes[i] = binaryStr.charCodeAt(i);
    }
    return new TextDecoder('utf-8').decode(bytes);
}

export default showSolutionArticle;
