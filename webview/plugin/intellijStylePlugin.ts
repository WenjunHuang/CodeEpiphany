import type { Plugin } from 'vite';

/**
 * Vite 插件：自动注入样式表链接和重载功能
 */
export function intellijStylePlugin(): Plugin {
  return {
    name: 'intellij-style-plugin',
    transformIndexHtml: {
      order:"pre",
      handler: (html) => {
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
              attrs: {
                type: 'module',
              },
              children: `
            import 'normalize-css/normalize.css'
            import 'overlayscrollbars/overlayscrollbars.css'
            import {OverlayScrollbars} from "overlayscrollbars";
            
            const MAX_ZOOM = 500;
            const MIN_ZOOM = 25;


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
              

    OverlayScrollbars(document.body, {});

    const gState = {
        'zoom': 100,
    }

    function setZoom(zoom = 100) {
        zoom = normalizeZoom(zoom);

        if (zoom === 100) {
            document.getElementById("container").style.zoom = '';
        } else {
            document.getElementById("container").style.zoom = \`\${zoom}%\`;
        }

        gState.zoom = zoom;

        _updateInfo();
    }

    function zoomIn() {
        setZoom(gState.zoom * 1.2);
    }

    function zoomOut() {
        setZoom(gState.zoom / 1.2);
    }

    function actualZoom() {
        setZoom(100);
    }

    function normalizeZoom(zoom) {
        zoom = Math.min(Math.max(zoom, MIN_ZOOM), MAX_ZOOM);
        return zoom;
    }

    let _init = function () {
        // OverlayScrollbars(document.body, {});
        setZoom(gState.zoom);
        window.removeEventListener('load', _init);
    }

    window.addEventListener('load', _init);

    function _updateInfo() {
        let info = {
            'zoom': gState.zoom,
            'canZoomIn': gState.zoom < MAX_ZOOM,
            'canZoomOut': gState.zoom > MIN_ZOOM,
        }
        window.sendInfo && window.sendInfo(JSON.stringify(info));
    }

    window.setZoom = setZoom;
    window.zoomIn = zoomIn;
    window.zoomOut = zoomOut;
    window.actualZoom = actualZoom;

              window.reloadStyles = reloadStyles;
            `,
              injectTo: 'head'
            }
          ]
        };
      }
    }
  };
} 