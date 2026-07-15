import { useEffect, useRef } from 'react';

interface WaterEffectProps {
  imageUrl: string;
  className?: string;
  /** Optional id used by IntersectionObserver for off-screen pause. */
  observerRoot?: HTMLElement | null;
}

const VS_SOURCE = `
  attribute vec2 a_position;
  attribute vec2 a_texCoord;
  varying vec2 v_texCoord;
  void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
  }
`;

/**
 * Fragment shader: distorts the image only in the lower 45% of the canvas,
 * simulating a calm pool reflection. Very small amplitude, very slow wave.
 */
const FS_SOURCE = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform float u_time;
  uniform vec2 u_resolution;
  varying vec2 v_texCoord;

  void main() {
    vec2 coord = v_texCoord;

    // Water is in the bottom 45% of the frame.
    if (coord.y < 0.45) {
      float depth = (0.45 - coord.y) / 0.45;

      float wave1 = sin(coord.y * 30.0 + u_time * 1.5) * 0.005;
      float wave2 = sin(coord.x * 20.0 + coord.y * 10.0 + u_time * 1.0) * 0.008;
      float wave3 = cos(coord.x * 40.0 - u_time * 2.0) * 0.003;

      float rippleX = (wave1 + wave2 + wave3) * depth;
      float rippleY = (wave1 - wave2) * depth * 0.5;

      coord.x += rippleX;
      coord.y += rippleY;
    }

    gl_FragColor = texture2D(u_image, coord);
  }
`;

function compileShader(
  gl: WebGLRenderingContext,
  type: number,
  source: string,
): WebGLShader | null {
  const shader = gl.createShader(type);
  if (!shader) return null;
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    gl.deleteShader(shader);
    return null;
  }
  return shader;
}

function createProgram(gl: WebGLRenderingContext): WebGLProgram | null {
  const vs = compileShader(gl, gl.VERTEX_SHADER, VS_SOURCE);
  const fs = compileShader(gl, gl.FRAGMENT_SHADER, FS_SOURCE);
  if (!vs || !fs) return null;
  const program = gl.createProgram();
  if (!program) return null;
  gl.attachShader(program, vs);
  gl.attachShader(program, fs);
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    gl.deleteProgram(program);
    return null;
  }
  return program;
}

function pushQuadUVs(
  gl: WebGLRenderingContext,
  buffer: WebGLBuffer,
  uMin: number,
  uMax: number,
  vMin: number,
  vMax: number,
) {
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([
      uMin, vMax,
      uMax, vMax,
      uMin, vMin,
      uMin, vMin,
      uMax, vMax,
      uMax, vMin,
    ]),
    gl.STATIC_DRAW,
  );
}

/**
 * Cinematic pool reflection layer rendered with a tiny WebGL shader.
 * Falls back gracefully: a static <img> sits behind it in the parent
 * component, so if this canvas ever fails to initialize, the user sees
 * the same composition minus the ripple (no broken UI).
 */
export function WaterEffect({ imageUrl, className }: WaterEffectProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fallbackRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const fallback = fallbackRef.current;
    if (!canvas) return;

    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const gl =
      (canvas.getContext('webgl', { antialias: true, premultipliedAlpha: false }) as WebGLRenderingContext | null) ||
      (canvas.getContext('experimental-webgl') as WebGLRenderingContext | null);

    if (!gl) {
      if (fallback) fallback.style.opacity = '1';
      canvas.style.display = 'none';
      return;
    }

    if (reduce) {
      if (fallback) fallback.style.opacity = '1';
      canvas.style.display = 'none';
      return;
    }

    const program = createProgram(gl);
    if (!program) {
      if (fallback) fallback.style.opacity = '1';
      canvas.style.display = 'none';
      return;
    }
    gl.useProgram(program);

    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([
        -1.0, -1.0,
        1.0, -1.0,
        -1.0, 1.0,
        -1.0, 1.0,
        1.0, -1.0,
        1.0, 1.0,
      ]),
      gl.STATIC_DRAW,
    );
    const positionLoc = gl.getAttribLocation(program, 'a_position');
    gl.enableVertexAttribArray(positionLoc);
    gl.vertexAttribPointer(positionLoc, 2, gl.FLOAT, false, 0, 0);

    const texCoordBuffer = gl.createBuffer();
    pushQuadUVs(gl, texCoordBuffer, 0, 1, 0, 1);
    const texCoordLoc = gl.getAttribLocation(program, 'a_texCoord');
    gl.enableVertexAttribArray(texCoordLoc);
    gl.vertexAttribPointer(texCoordLoc, 2, gl.FLOAT, false, 0, 0);

    const texture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, texture);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);

    const timeLoc = gl.getUniformLocation(program, 'u_time');

    const image = new Image();
    image.decoding = 'async';
    image.src = imageUrl;

    let animationId = 0;
    let inView = true;

    const resize = () => {
      if (!inView) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const cssWidth = canvas.clientWidth;
      const cssHeight = canvas.clientHeight;
      const targetWidth = Math.max(1, Math.floor(cssWidth * dpr));
      const targetHeight = Math.max(1, Math.floor(cssHeight * dpr));
      if (canvas.width !== targetWidth || canvas.height !== targetHeight) {
        canvas.width = targetWidth;
        canvas.height = targetHeight;
        gl.viewport(0, 0, canvas.width, canvas.height);

        if (!image.width || !image.height) {
          pushQuadUVs(gl, texCoordBuffer, 0, 1, 0, 1);
          return;
        }
        const imageAspect = image.width / image.height;
        const canvasAspect = cssWidth / cssHeight;
        let uMin = 0, uMax = 1, vMin = 0, vMax = 1;
        if (canvasAspect > imageAspect) {
          const scale = canvasAspect / imageAspect;
          const offset = (1.0 - 1.0 / scale) / 2.0;
          vMin = offset;
          vMax = 1.0 - offset;
        } else {
          const scale = imageAspect / canvasAspect;
          const offset = (1.0 - 1.0 / scale) / 2.0;
          uMin = offset;
          uMax = 1.0 - offset;
        }
        pushQuadUVs(gl, texCoordBuffer, uMin, uMax, vMin, vMax);
      }
    };

    image.onload = () => {
      gl.bindTexture(gl.TEXTURE_2D, texture);
      gl.texImage2D(
        gl.TEXTURE_2D,
        0,
        gl.RGBA,
        gl.RGBA,
        gl.UNSIGNED_BYTE,
        image,
      );

      window.addEventListener('resize', resize);
      resize();
      if (fallback) fallback.style.opacity = '0';

      const startTime = performance.now();
      const render = (now: number) => {
        if (!inView) {
          animationId = requestAnimationFrame(render);
          return;
        }
        gl.uniform1f(timeLoc, (now - startTime) * 0.001);
        gl.drawArrays(gl.TRIANGLES, 0, 6);
        animationId = requestAnimationFrame(render);
      };
      animationId = requestAnimationFrame(render);
    };

    image.onerror = () => {
      if (fallback) fallback.style.opacity = '1';
      canvas.style.display = 'none';
    };

    const observer =
      typeof IntersectionObserver !== 'undefined'
        ? new IntersectionObserver(
            (entries) => {
              for (const entry of entries) {
                inView = entry.isIntersecting;
              }
            },
            { threshold: 0 },
          )
        : null;
    if (observer) observer.observe(canvas);

    return () => {
      cancelAnimationFrame(animationId);
      window.removeEventListener('resize', resize);
      if (observer) observer.disconnect();
      const ext = gl.getExtension('WEBGL_lose_context');
      if (ext) ext.loseContext();
      if (texture) gl.deleteTexture(texture);
      if (program) gl.deleteProgram(program);
    };
  }, [imageUrl]);

  return (
    <>
      <img
        ref={fallbackRef}
        src={imageUrl}
        alt=""
        aria-hidden="true"
        decoding="async"
        fetchPriority="high"
        loading="eager"
        className={`h-full w-full object-cover ${className ?? ''}`}
        style={{ transition: 'opacity 600ms cubic-bezier(0.16, 1, 0.3, 1)' }}
      />
      <canvas
        ref={canvasRef}
        aria-hidden="true"
        className={`absolute inset-0 h-full w-full ${className ?? ''}`}
      />
    </>
  );
}

export default WaterEffect;
