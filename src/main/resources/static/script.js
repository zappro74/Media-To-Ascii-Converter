const byId = (id) => document.getElementById(id);

function blobDownload(filename, blob)
{
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function downloadTextFile(filename, text)
{
  blobDownload(filename, new Blob([text], { type: 'text/plain;charset=utf-8' }));
}

function renderAsciiFrame(outputEl, infoEl, frames, index)
{
  if (!frames || frames.length === 0)
  {
    outputEl.innerHTML = '';
    if (infoEl) infoEl.textContent = 'Frame 0 / 0';
    return;
  }
  const frame = frames[index];
  const text = Array.isArray(frame) ? frame.join('\n') : (typeof frame === 'string' ? frame : '');
  outputEl.innerHTML = ansiToHtml(text);
  outputEl.hidden = false;
  if (infoEl) infoEl.textContent = 'Frame ' + (index + 1) + ' / ' + frames.length;
}

function getFilenameBase(fileInput, fallback)
{
  if (fileInput && fileInput.files && fileInput.files[0])
  {
    const original = fileInput.files[0].name || fallback;
    const base = original.replace(/\.[^.]+$/, '');
    return base || fallback;
  }
  return fallback;
}

function ansiToHtml(text)
{
  const ansiRegex = /\u001B\[38;2;(\d+);(\d+);(\d+)m(.?)\u001B\[0m/g;
  return text.replace(ansiRegex, (match, r, g, b, char) => {
    return `<span style="color:rgb(${r},${g},${b})">${char}</span>`;
  });
}

// Produce plain ASCII text by stripping ANSI color codes but keeping characters
function stripAnsi(text)
{
  const ansiRegex = /\u001B\[38;2;\d+;\d+;\d+m(.?)\u001B\[0m/g;
  return text.replace(ansiRegex, (match, char) => char);
}

function setupUnifiedForm()
{
  const form = byId('unifiedForm');
  if (!form) return;

  const out = byId('unifiedOutput');
  const controls = byId('unifiedControls');
  const prev = byId('unifiedPrev');
  const next = byId('unifiedNext');
  const info = byId('unifiedFrameInfo');
  const downloadFrameBtn = byId('unifiedDownloadFrameBtn');
  const downloadFullBtn = byId('unifiedDownloadFullBtn');
  const widthSlider = byId('unifiedWidth');
  const widthValue = byId('unifiedWidthValue');

  let frames = [];
  let index = 0;
  let mediaType = 'image';
  let filenameBase = 'output';
  let imageText = '';

  if (widthSlider && widthValue)
  {
    widthValue.textContent = widthSlider.value;
    widthSlider.addEventListener('input', () => { widthValue.textContent = widthSlider.value; });
  }

  form.onsubmit = async (e) =>
  {
    e.preventDefault();
    out.hidden = true;
    out.textContent = '';
    if (controls) controls.hidden = true;
    if (downloadFrameBtn) { downloadFrameBtn.disabled = true; downloadFrameBtn.hidden = true; }
    if (downloadFullBtn) { downloadFullBtn.disabled = true; downloadFullBtn.hidden = true; }
    frames = [];
    index = 0;
    imageText = '';

    const data = new FormData(form);
    let w = 120;
    if (widthSlider)
    {
      const v = parseInt(widthSlider.value, 10);
      if (!isNaN(v)) w = Math.max(50, Math.min(300, v));
    }
    data.set('width', String(w));

    const fileInput = form.querySelector('input[name="file"]');
    filenameBase = getFilenameBase(fileInput, 'output');

    try
    {
      const resp = await fetch('/convertAuto', { method: 'POST', body: data });
      const json = await resp.json();

      if (!resp.ok)
      {
        out.textContent = 'Error: ' + (json.message || 'Conversion failed');
        out.hidden = false;
        return;
      }

      mediaType = json.type;

      if (mediaType === 'image')
      {
        imageText = json.ascii;
        out.innerHTML = ansiToHtml(imageText);
        out.hidden = false;
        if (downloadFrameBtn) { downloadFrameBtn.disabled = false; downloadFrameBtn.hidden = false; }
      }
      else if (mediaType === 'gif' || mediaType === 'video')
      {
        frames = json.frames || [];
        if (frames.length > 0)
        {
          index = 0;
          renderAsciiFrame(out, info, frames, index);
          if (controls) controls.hidden = false;
          if (downloadFrameBtn) { downloadFrameBtn.disabled = false; downloadFrameBtn.hidden = false; }
          if (downloadFullBtn) { downloadFullBtn.disabled = false; downloadFullBtn.hidden = false; }
        }
        else
        {
          out.textContent = 'No frames returned';
          out.hidden = false;
        }
      }
    }
    catch (err)
    {
      out.textContent = 'Request failed: ' + err;
      out.hidden = false;
    }
  };

  if (prev)
  {
    prev.onclick = () =>
    {
      if (mediaType === 'image' || frames.length === 0) return;
      if (index > 0) { index -= 1; renderAsciiFrame(out, info, frames, index); }
    };
  }

  if (next)
  {
    next.onclick = () =>
    {
      if (mediaType === 'image' || frames.length === 0) return;
      if (index < frames.length - 1) { index += 1; renderAsciiFrame(out, info, frames, index); }
    };
  }

  if (downloadFrameBtn)
  {
    downloadFrameBtn.onclick = () =>
    {
      if (mediaType === 'image')
      {
        downloadTextFile(filenameBase + ' (ASCII).txt', stripAnsi(imageText));
      }
      else if (frames.length > 0)
      {
        const frame = frames[index];
        const text = Array.isArray(frame) ? frame.join('\n') : (typeof frame === 'string' ? frame : '');
        downloadTextFile(filenameBase + ' (frame ' + (index + 1) + ').txt', stripAnsi(text));
      }
    };
  }

  if (downloadFullBtn)
  {
    downloadFullBtn.onclick = async () =>
    {
      if (mediaType === 'image' || frames.length === 0) return;
      downloadFullBtn.disabled = true;
      const originalText = downloadFullBtn.textContent;
      downloadFullBtn.textContent = 'Generating...';

      try
      {
        const data = new FormData(form);
        let w = 120;
        if (widthSlider)
        {
          const v = parseInt(widthSlider.value, 10);
          if (!isNaN(v)) w = Math.max(50, Math.min(300, v));
        }
        data.set('width', String(w));
        const previewFontPx = parseInt(window.getComputedStyle(out).fontSize, 10);
        if (!isNaN(previewFontPx) && previewFontPx > 0) {
          data.set('fontSize', String(previewFontPx));
        }

        const endpoint = mediaType === 'video' ? '/exportVideo' : '/exportGif';
        const ext = mediaType === 'video' ? '.mp4' : '.gif';
        const resp = await fetch(endpoint, { method: 'POST', body: data });
        if (!resp.ok) throw new Error('Export failed');
        const blob = await resp.blob();
        blobDownload(filenameBase + ' (ASCII)' + ext, blob);
      }
      catch (e)
      {
        alert('Export failed');
      }
      finally
      {
        downloadFullBtn.disabled = false;
        downloadFullBtn.textContent = originalText;
      }
    };
  }
}

function setupImageForm()
{
  const form = byId('imageForm');
  if (!form) return;

  const out = byId('imageOutput');
  const ramp = byId('imageRamp');
  const widthSlider = byId('imageWidth');
  const widthValue = byId('imageWidthValue');
  const downloadBtn = byId('imageDownloadBtn');

  let imageText = '';
  let imageFilename = 'ascii.txt';

  if (widthSlider && widthValue)
  {
    widthValue.textContent = widthSlider.value;
    widthSlider.addEventListener('input', () => { widthValue.textContent = widthSlider.value; });
  }

  form.onsubmit = (e) =>
  {
    e.preventDefault();
    out.hidden = true;
    out.textContent = '';
    downloadBtn.disabled = true;
    downloadBtn.hidden = true;

    const data = new FormData(form);
    let w = 120;
    if (widthSlider)
    {
      const v = parseInt(widthSlider.value, 10);
      if (!isNaN(v)) w = Math.max(50, Math.min(300, v));
    }
    data.set('width', String(w));
    if (ramp && ramp.value) data.set('ramp', ramp.value);

    const fileInput = form.querySelector('input[name="file"]');
    const base = getFilenameBase(fileInput, 'ascii');
    imageFilename = base + '.txt';

    fetch('/convert', { method: 'POST', body: data })
      .then((resp) => resp.text().then((text) => ({ ok: resp.ok, text })))
      .then(({ ok, text }) =>
      {
        if (ok)
        {
          imageText = text;
          out.innerHTML = ansiToHtml(text);
          out.hidden = false;
          downloadBtn.disabled = false;
          downloadBtn.hidden = false;
        }
        else
        {
          imageText = '';
          out.textContent = 'Error: ' + text;
          out.hidden = false;
          downloadBtn.disabled = true;
          downloadBtn.hidden = true;
        }
      })
      .catch((err) =>
      {
        out.textContent = 'Request failed: ' + err;
        out.hidden = false;
        imageText = '';
        downloadBtn.disabled = true;
        downloadBtn.hidden = true;
      });
  };

  downloadBtn.onclick = () => 
  {
    if (!imageText) return;
    const colorInput = form.querySelector('input[name="color"]');
    const keepColor = !!(colorInput && colorInput.checked);
    const content = keepColor ? imageText : stripAnsi(imageText);
    downloadTextFile(imageFilename.replace(/\.txt$/, ' (ASCII).txt'), content);
  };
}

function setupFrameFlow(opts)
{
  const 
  {
    formId, outputId, controlsId, prevId, nextId, infoId,
    downloadFrameBtnId, downloadAllBtnId,
    convertEndpoint, exportEndpoint,
    filenameFallback, convertBtnId, fileInputSelector,
    exportSuffix, widthSliderId, widthValueId
  } = opts;

  const form = byId(formId);
  if (!form) return;

  const out = byId(outputId);
  const controls = byId(controlsId);
  const prev = byId(prevId);
  const next = byId(nextId);
  const info = byId(infoId);
  const downloadFrameBtn = byId(downloadFrameBtnId);
  const downloadAllBtn = byId(downloadAllBtnId);
  const convertBtn = convertBtnId ? byId(convertBtnId) : null;
  const fileInput = fileInputSelector ? form.querySelector(fileInputSelector) : form.querySelector('input[name="file"]');
  const widthSlider = widthSliderId ? byId(widthSliderId) : null;
  const widthValue = widthValueId ? byId(widthValueId) : null;

  let frames = [];
  let index = 0;
  let filenameBase = filenameFallback || 'output';

  if (widthSlider && widthValue)
  {
    widthValue.textContent = widthSlider.value;
    widthSlider.addEventListener('input', () => { widthValue.textContent = widthSlider.value; });
  }

  if (convertBtn && fileInput)
  {
    convertBtn.disabled = !(fileInput.files && fileInput.files.length > 0);
    fileInput.addEventListener('change', () =>
    {
      convertBtn.disabled = !(fileInput.files && fileInput.files.length > 0);
      filenameBase = getFilenameBase(fileInput, filenameFallback || 'output');
    });
  }
  else if (fileInput)
  {
    filenameBase = getFilenameBase(fileInput, filenameFallback || 'output');
  }

  form.onsubmit = (e) =>
  {
    e.preventDefault();
    out.hidden = true;
    out.textContent = '';
    if (controls) controls.hidden = true;
    frames = [];
    index = 0;
    if (downloadFrameBtn) { downloadFrameBtn.disabled = true; downloadFrameBtn.hidden = true; }
    if (downloadAllBtn) { downloadAllBtn.disabled = true; downloadAllBtn.hidden = true; }

    const data = new FormData(form);
    let w = 120;
    if (widthSlider)
    {
      const v = parseInt(widthSlider.value, 10);
      if (!isNaN(v)) w = Math.max(50, Math.min(300, v));
    }
    data.set('width', String(w));

    fetch(convertEndpoint, { method: 'POST', body: data })
      .then((resp) => resp.json().then((json) => ({ ok: resp.ok, json })))
      .then(({ ok, json }) =>
      {
        if (ok && Array.isArray(json) && json.length > 0)
        {
          frames = json;
          index = 0;
          renderAsciiFrame(out, info, frames, index);
          if (controls) controls.hidden = false;
          out.hidden = false;
          if (downloadFrameBtn) { downloadFrameBtn.disabled = false; downloadFrameBtn.hidden = false; }
          if (downloadAllBtn) { downloadAllBtn.disabled = false; downloadAllBtn.hidden = false; }
        }
        else
        {
          out.textContent = 'No frames returned or error.';
          out.hidden = false;
        }
      })
      .catch((err) =>
      {
        out.textContent = 'Request failed: ' + err;
        out.hidden = false;
      });
  };

  if (prev)
  {
    prev.onclick = () =>
    {
      if (!frames || frames.length === 0) return;
      if (index > 0) { index -= 1; renderAsciiFrame(out, info, frames, index); }
    };
  }

  if (next)
  {
    next.onclick = () =>
    {
      if (!frames || frames.length === 0) return;
      if (index < frames.length - 1) { index += 1; renderAsciiFrame(out, info, frames, index); }
    };
  }

  if (downloadFrameBtn)
  {
    downloadFrameBtn.onclick = () =>
    {
      if (!frames || frames.length === 0) return;
      const frame = frames[index];
      const text = Array.isArray(frame) ? frame.join('\n') : (typeof frame === 'string' ? frame : '');
      if (!text) return;
      const colorInput = form.querySelector('input[name="color"]');
      const keepColor = !!(colorInput && colorInput.checked);
      const content = keepColor ? text : stripAnsi(text);
      downloadTextFile(filenameBase + ' (frame ' + (index + 1) + ').txt', content);
    };
  }

  if (downloadAllBtn && exportEndpoint)
  {
    downloadAllBtn.onclick = async () =>
    {
      if (!frames || frames.length === 0) return;
      downloadAllBtn.disabled = true;
      const originalText = downloadAllBtn.textContent;
      downloadAllBtn.textContent = 'Generating...';
      try
      {
        const data = new FormData(form);
        let w = 120;
        if (widthSlider) {
          const v = parseInt(widthSlider.value, 10);
          if (!isNaN(v)) w = Math.max(50, Math.min(300, v));
        }
        data.set('width', String(w));
        const previewFontPx = parseInt(window.getComputedStyle(out).fontSize, 10);
        if (!isNaN(previewFontPx) && previewFontPx > 0) {
          data.set('fontSize', String(previewFontPx));
        }
        const resp = await fetch(exportEndpoint, { method: 'POST', body: data });
        if (!resp.ok) throw new Error('Export failed');
        const blob = await resp.blob();
        const ext = (exportSuffix && exportSuffix.includes('.')) ? exportSuffix.slice(exportSuffix.indexOf('.')) : '';
        const name = filenameBase + ' (ASCII)' + ext;
        blobDownload(name, blob);
      }
      catch (e)
      {
        alert('Export failed');
      }
      finally
      {
        downloadAllBtn.disabled = false;
        downloadAllBtn.textContent = originalText;
      }
    };
  }
}

setupUnifiedForm();