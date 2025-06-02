import type { Plugin } from 'vite';

/**
 * Vite 插件：自动注入样式表链接和重载功能
 */
export function intellijStylePlugin(): Plugin {
  return {
    name: 'intellij-style-plugin',
    transformIndexHtml(html) {
      return {
        html,
        tags: [
          {
            tag: 'link',
            attrs: {
              id: 'intellijStyle',
              rel: 'stylesheet',
              type: 'text/css',
              href: '/intellijStyle.css'
            },
            injectTo: 'head'
          },
          {
            tag: 'script',
            children: `
              function reloadStyles() {
                const styleElement = document.getElementById('intellijStyle');
                if (styleElement) {
                  styleElement.href = _setTimestamp(styleElement.href);
                }
              }

              function _setTimestamp(url) {
                let patchedUrl = new URL(url);
                patchedUrl.searchParams.set('timestamp', new Date().getTime().toString());
                return patchedUrl.toString();
              }

              window.reloadStyles = reloadStyles;
            `,
            injectTo: 'head'
          }
        ]
      };
    }
  };
} 