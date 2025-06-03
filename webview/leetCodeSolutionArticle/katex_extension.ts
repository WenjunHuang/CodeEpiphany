import katex, {type KatexOptions} from 'katex'
import 'katex/dist/katex.css'
import type {MarkedExtension, TokenizerAndRendererExtension} from 'marked'

export default function (options: KatexOptions = {}): MarkedExtension {
    return {
        extensions: [
            inlineKatex(options),
            blockKatex(options)
        ]
    }
}

function inlineKatex(options: KatexOptions): TokenizerAndRendererExtension {
    return {
        name: 'inlineKatex',
        level: 'inline',
        start(src: string) {
            return src.indexOf('$')
        },
        tokenizer(src: string, _tokens) {
            const match = src.match(/^\$([^\$\n]+?)\$/)
            if (match) {
                return {
                    type: 'inlineKatex',
                    raw: match[0],
                    text: match[1].trim()
                }
            } else {
                return undefined
            }
        },
        renderer(token) {
            return katex.renderToString(token.text, {...options, displayMode: false})
        }
    }
}

function blockKatex(options: KatexOptions): TokenizerAndRendererExtension {
    return {
        name: 'blockKatex',
        level: 'block',
        start(src: string) {
            return src.indexOf('$$')
        },
        tokenizer(src: string, _tokens) {
            const match = src.match(/^\$\$+\n([^\$]+?)\n\$\$/)
            if (match) {
                return {
                    type: 'blockKatex',
                    raw: match[0],
                    text: match[1].trim()
                }
            } else {
                return undefined;
            }
        },
        renderer(token) {
            options.displayMode = true
            return `<p>${katex.renderToString(token.text, {...options, displayMode: true})}</p>`
        }
    }
}