'use client';

import { useRef, useState, useCallback, useEffect } from 'react';

interface Props {
  src: string;
  className?: string;
}

export default function PanoramaViewer({ src, className = '' }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);

  const handleMouseDown = (e: React.MouseEvent) => {
    setDragging(true);
    setStartPos({ x: e.clientX - offset.x, y: e.clientY - offset.y });
  };

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!dragging) return;
    setOffset({ x: e.clientX - startPos.x, y: Math.max(-200, Math.min(200, e.clientY - startPos.y)) });
  }, [dragging, startPos]);

  const handleMouseUp = useCallback(() => setDragging(false), []);

  useEffect(() => {
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [handleMouseMove, handleMouseUp]);

  // Touch support
  const handleTouchStart = (e: React.TouchEvent) => {
    const touch = e.touches[0];
    setDragging(true);
    setStartPos({ x: touch.clientX - offset.x, y: touch.clientY - offset.y });
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!dragging) return;
    const touch = e.touches[0];
    setOffset({ x: touch.clientX - startPos.x, y: Math.max(-200, Math.min(200, touch.clientY - startPos.y)) });
  };

  // Gyroscope support for mobile
  useEffect(() => {
    const handleOrientation = (e: DeviceOrientationEvent) => {
      if (e.alpha !== null && e.gamma !== null) {
        setOffset(prev => ({
          x: -(e.alpha || 0) * 3,
          y: -(e.gamma || 0) * 3
        }));
      }
    };

    if (typeof DeviceOrientationEvent !== 'undefined') {
      window.addEventListener('deviceorientation', handleOrientation);
    }
    return () => window.removeEventListener('deviceorientation', handleOrientation);
  }, []);

  return (
    <div
      ref={containerRef}
      className={`relative overflow-hidden cursor-grab active:cursor-grabbing rounded-xl ${className}`}
      style={{ height: '400px' }}
      onMouseDown={handleMouseDown}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={() => setDragging(false)}
      onWheel={(e) => setZoom(prev => Math.max(1, Math.min(3, prev + (e.deltaY > 0 ? -0.1 : 0.1))))}
    >
      <img
        src={src}
        alt="360 panorama view"
        className="absolute select-none"
        draggable={false}
        style={{
          width: `${300 * zoom}%`,
          height: `${120 * zoom}%`,
          transform: `translate(${offset.x}px, ${offset.y}px)`,
          transition: dragging ? 'none' : 'transform 0.1s ease-out',
          objectFit: 'cover',
        }}
      />
      {/* Controls */}
      <div className="absolute bottom-4 left-4 flex gap-2">
        <button
          onClick={() => setZoom(prev => Math.min(3, prev + 0.3))}
          className="w-10 h-10 bg-white/90 rounded-full flex items-center justify-center shadow text-lg font-bold"
        >+</button>
        <button
          onClick={() => setZoom(prev => Math.max(1, prev - 0.3))}
          className="w-10 h-10 bg-white/90 rounded-full flex items-center justify-center shadow text-lg font-bold"
        >-</button>
        <button
          onClick={() => { setOffset({ x: 0, y: 0 }); setZoom(1); }}
          className="px-3 h-10 bg-white/90 rounded-full flex items-center justify-center shadow text-sm"
        >Reset</button>
      </div>
      {/* Badge */}
      <div className="absolute top-4 left-4 bg-black/60 text-white px-3 py-1 rounded-full text-sm flex items-center gap-1">
        <span>🔄</span> 360° View — Drag to explore
      </div>
    </div>
  );
}
