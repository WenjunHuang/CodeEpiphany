import 'normalize-css/normalize.css'

declare global {
    interface Window {
        setZoom: (zoom: number) => void;
        zoomIn: () => void;
        zoomOut: () => void;
        actualZoom: () => void;
        reloadStyles: () => void;
        sendInfo: (info: string) => void;
    }

    let OverlayScrollbarsGlobal: {
        OverlayScrollbars: (element: Element | string, options?: any) => any;
    };
}

const MAX_ZOOM = 500;
const MIN_ZOOM = 25;

const initialize = () => {
    // const _ = (new URL(window.location.href).searchParams.get('debug') != null);

    OverlayScrollbarsGlobal.OverlayScrollbars(document.body, {});

    const gState = {
        'zoom': 100,
    }


    const gStyles = {
        'description': (document.getElementById('descriptionStyle')! as HTMLLinkElement),
    }

    function setZoom(zoom = 100) {
        zoom = normalizeZoom(zoom);

        if (zoom === 100) {
            document.getElementById("container")!.style.zoom = '';
        } else {
            document.getElementById("container")!.style.zoom = `${zoom}%`;
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

    function normalizeZoom(zoom: number) {
        zoom = Math.min(Math.max(zoom, MIN_ZOOM), MAX_ZOOM);
        return zoom;
    }

    let _init = function () {
        OverlayScrollbarsGlobal.OverlayScrollbars(document.body, {});
        setZoom(gState.zoom);
        window.removeEventListener('load', _init);
    }

    window.addEventListener('load', _init);


    function reloadStyles() {
        gStyles.description.href = _setTimestamp(gStyles.description.href);
    }

    function _setTimestamp(url:string) {
        let patchedUrl = new URL(url);
        patchedUrl.searchParams.set('timestamp', new Date().getTime().toString());
        return patchedUrl.toString();
    }

    function _updateInfo() {
        let info = {
            'zoom': gState.zoom,
            'canZoomIn': gState.zoom < MAX_ZOOM,
            'canZoomOut': gState.zoom > MIN_ZOOM,
        }
        window.sendInfo(JSON.stringify(info));
    }

    window.setZoom = setZoom;
    window.zoomIn = zoomIn;
    window.zoomOut = zoomOut;
    window.reloadStyles = reloadStyles;
    window.actualZoom = actualZoom;
}

export default initialize;