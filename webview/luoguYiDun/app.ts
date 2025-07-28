import CryptoJS from 'crypto-js';
import './app.css';

declare global {
    interface Window {
        showYiDunCaptcha: (code: string, state: string, onVerify: (result: string) => void) => void;
        // provided by NECaptcha
        initNECaptcha: (config: any) => void;
    }
}


function base64url(wordArray: CryptoJS.lib.WordArray) {
    let b64 = CryptoJS.enc.Base64.stringify(wordArray);
    return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

window.showYiDunCaptcha = async function (code: string, state: string, onVerify: (result: string) => void) {
    const key = CryptoJS.enc.Hex.parse(state);
    const hmac = CryptoJS.HmacSHA256(code, key);
    const extra = base64url(hmac);

    window.initNECaptcha({
        captchaId: "54b8e9e1557a423b8c73e7f80294dbe3",
        mode: "embed",
        extraData: extra,
        lang: "zh-CN",
        width: "300px",
        timeout: 10000,
        closeEnable: false,
        feedbackEnable: false,
        defaultFallback: false,
        ipv6: true,
        apiVersion: 2,
        onVerify: (_: any, t: any) => {
            if (t != null && t.validate) {
                const result = t.validate;
                onVerify(result);
            }
        },
        element: document.getElementById('container'),
    })
}
