'use client';

import { useState } from 'react';

interface Room {
  id: string;
  name: string;
  x: number; // percentage position
  y: number;
  width: number;
  height: number;
  photoIndex?: number; // index into media array to jump to
}

interface Props {
  floorPlanUrl: string;
  rooms?: Room[];
  onRoomClick?: (room: Room) => void;
  className?: string;
}

export default function FloorPlanViewer({ floorPlanUrl, rooms = [], onRoomClick, className = '' }: Props) {
  const [hoveredRoom, setHoveredRoom] = useState<string | null>(null);
  const [zoom, setZoom] = useState(1);

  return (
    <div className={`relative bg-gray-50 rounded-xl overflow-hidden ${className}`}>
      {/* Header */}
      <div className="bg-white border-b px-4 py-3 flex items-center justify-between">
        <h3 className="font-semibold text-gray-900 flex items-center gap-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
          </svg>
          Floor Plan
        </h3>
        <div className="flex gap-1">
          <button onClick={() => setZoom(prev => Math.min(2, prev + 0.2))} className="px-2 py-1 bg-gray-100 rounded text-sm">+</button>
          <button onClick={() => setZoom(prev => Math.max(0.5, prev - 0.2))} className="px-2 py-1 bg-gray-100 rounded text-sm">-</button>
        </div>
      </div>

      {/* Floor plan image with room overlays */}
      <div className="relative p-4 overflow-auto" style={{ maxHeight: '500px' }}>
        <div style={{ transform: `scale(${zoom})`, transformOrigin: 'top left', transition: 'transform 0.2s' }}>
          <img src={floorPlanUrl} alt="Floor plan" className="w-full" draggable={false} />

          {/* Room hotspots */}
          {rooms.map(room => (
            <button
              key={room.id}
              onClick={() => onRoomClick?.(room)}
              onMouseEnter={() => setHoveredRoom(room.id)}
              onMouseLeave={() => setHoveredRoom(null)}
              className={`absolute border-2 rounded transition-all cursor-pointer ${
                hoveredRoom === room.id
                  ? 'border-orange-500 bg-orange-500/20'
                  : 'border-blue-400/50 bg-blue-400/10 hover:bg-blue-400/20'
              }`}
              style={{
                left: `${room.x}%`,
                top: `${room.y}%`,
                width: `${room.width}%`,
                height: `${room.height}%`,
              }}
            >
              <span className={`absolute -top-6 left-1/2 -translate-x-1/2 px-2 py-0.5 rounded text-xs font-medium whitespace-nowrap transition-opacity ${
                hoveredRoom === room.id ? 'opacity-100 bg-gray-900 text-white' : 'opacity-0'
              }`}>
                {room.name}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Room legend */}
      {rooms.length > 0 && (
        <div className="border-t px-4 py-2 flex flex-wrap gap-2">
          {rooms.map(room => (
            <button
              key={room.id}
              onClick={() => onRoomClick?.(room)}
              className="px-3 py-1 bg-gray-100 hover:bg-gray-200 rounded-full text-xs font-medium transition-colors"
            >
              {room.name}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
