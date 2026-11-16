import React, { useMemo } from 'react';

// Nền thương hiệu HNQ Media: dải ruy-băng uốn lượn (như logo) + lớp bụi sáng mờ tĩnh hòa vào nền.

// Bộ sinh số giả ngẫu nhiên cố định để vị trí hạt không đổi giữa các lần render.
function rng(seed) {
  let s = seed;
  return () => {
    s = (s * 16807) % 2147483647;
    return (s - 1) / 2147483646;
  };
}

// Ba lớp: xa (nhỏ, mờ, chậm), giữa, gần (lớn hơn, nhòe nhẹ, sáng, nhanh) -> hiệu ứng thị sai.
const LAYERS = [
  { count: 90, size: [1, 2], opacity: [0.05, 0.16], blur: 0.6 },
  { count: 40, size: [2, 4], opacity: [0.05, 0.14], blur: 1.4 },
  { count: 14, size: [6, 14], opacity: [0.03, 0.08], blur: 4 },
];
const COLORS = ['#e0f2fe', '#bae6fd', '#c4b5fd', '#a5f3fc', '#ffffff'];

function buildParticles() {
  const rand = rng(20260);
  const out = [];
  LAYERS.forEach((l, li) => {
    for (let i = 0; i < l.count; i++) {
      const size = l.size[0] + rand() * (l.size[1] - l.size[0]);
      out.push({
        key: `${li}-${i}`,
        left: rand() * 100,
        top: rand() * 100,
        size,
        opacity: l.opacity[0] + rand() * (l.opacity[1] - l.opacity[0]),
        blur: l.blur,
        color: COLORS[Math.floor(rand() * COLORS.length)],
      });
    }
  });
  return out;
}

export function BrandBackground() {
  const particles = useMemo(buildParticles, []);

  return (
    <div aria-hidden="true" className="absolute inset-0 overflow-hidden bg-[#02040d]">
      <style>{`
        @keyframes hq-wave { 0%,100% { transform: translateX(0); } 50% { transform: translateX(-40px); } }
        /* Chỉ đổi độ đậm nhạt (opacity) của các lớp gradient phủ kín, không dịch chuyển nên không lộ mép.
           Các lớp đặt lệch vị trí và lệch chu kỳ, cross-fade với nhau nên vùng sáng như tự trôi một cách mượt. */
        @keyframes hq-fade-a { 0%,100% { opacity: 1; } 35% { opacity: .15; } 70% { opacity: .75; } }
        @keyframes hq-fade-b { 0%,100% { opacity: .1; } 40% { opacity: 1; } 75% { opacity: .35; } }
        @keyframes hq-fade-c { 0%,100% { opacity: .8; } 30% { opacity: .1; } 65% { opacity: 1; } }
        @keyframes hq-fade-d { 0%,100% { opacity: .2; } 50% { opacity: .9; } }
        @media (prefers-reduced-motion: reduce) { .hq-anim { animation: none !important; } }
      `}</style>

      {/* Các lớp gradient phủ kín, chỉ đổi độ đậm nhạt */}
      <div className="hq-anim absolute inset-0" style={{ animation: 'hq-fade-a 23s ease-in-out infinite', background: 'radial-gradient(ellipse 62% 52% at 20% 24%, rgba(59,110,245,0.95) 0%, rgba(37,80,220,0.55) 28%, rgba(16,36,120,0.25) 52%, transparent 75%)' }} />
      <div className="hq-anim absolute inset-0" style={{ animation: 'hq-fade-b 29s ease-in-out infinite', background: 'radial-gradient(ellipse 58% 48% at 38% 40%, rgba(59,110,245,0.9) 0%, rgba(37,80,220,0.5) 30%, rgba(16,36,120,0.2) 55%, transparent 78%)' }} />
      <div className="hq-anim absolute inset-0" style={{ animation: 'hq-fade-c 37s ease-in-out infinite', background: 'radial-gradient(ellipse 55% 40% at 76% 14%, rgba(37,70,200,0.75) 0%, rgba(30,58,170,0.3) 45%, transparent 75%)' }} />
      <div className="hq-anim absolute inset-0" style={{ animation: 'hq-fade-d 43s ease-in-out infinite', background: 'radial-gradient(ellipse 50% 36% at 62% 76%, rgba(76,50,190,0.5) 0%, rgba(40,30,120,0.2) 50%, transparent 78%)' }} />
      {/* Vệt sáng chéo: dải gradient mềm phủ kín, chỉ đổi độ đậm nhạt */}
      <div className="hq-anim absolute inset-0" style={{ animation: 'hq-fade-c 31s ease-in-out infinite', background: 'linear-gradient(158deg, transparent 18%, rgba(56,110,255,0.22) 30%, rgba(56,110,255,0.32) 36%, rgba(56,110,255,0.14) 44%, transparent 56%)' }} />
      {/* Tối dần xuống đáy (đứng yên) */}
      <div className="absolute inset-0" style={{ background: 'linear-gradient(180deg, rgba(2,4,13,0) 35%, #010208 100%)' }} />

      {/* Dải ruy-băng như logo */}
      <svg className="hq-anim absolute inset-x-[-5%] top-[18%] h-[70%] w-[110%]" viewBox="0 0 1440 600" preserveAspectRatio="none" style={{ animation: 'hq-wave 16s ease-in-out infinite' }}>
        <defs>
          <linearGradient id="rib1" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stopColor="#22d3ee" stopOpacity="0.55" />
            <stop offset="0.55" stopColor="#3b82f6" stopOpacity="0.45" />
            <stop offset="1" stopColor="#a855f7" stopOpacity="0.55" />
          </linearGradient>
          <linearGradient id="rib2" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stopColor="#38bdf8" stopOpacity="0.25" />
            <stop offset="1" stopColor="#c084fc" stopOpacity="0.4" />
          </linearGradient>
        </defs>
        <path d="M0 360 C 240 120, 480 140, 720 300 S 1200 520, 1440 220 L1440 330 C 1200 600, 960 420, 720 380 S 240 260, 0 470 Z" fill="url(#rib1)" />
        <path d="M0 470 C 260 300, 520 330, 760 430 S 1200 600, 1440 400 L1440 470 C 1180 660, 980 520, 760 500 S 260 420, 0 560 Z" fill="url(#rib2)" />
        <path d="M0 360 C 240 120, 480 140, 720 300 S 1200 520, 1440 220" fill="none" stroke="#e0f2fe" strokeOpacity="0.5" strokeWidth="1.5" />
      </svg>

      {/* Bụi sáng mờ, tĩnh, hòa vào nền */}
      {particles.map((p) => (
        <span
          key={p.key}
          className="absolute rounded-full mix-blend-screen"
          style={{
            left: `${p.left}%`,
            top: `${p.top}%`,
            width: p.size,
            height: p.size,
            background: p.color,
            opacity: p.opacity,
            filter: `blur(${p.blur}px)`,
          }}
        />
      ))}

      {/* Nhiễu hạt phim (film grain) như ảnh mẫu */}
      <svg className="pointer-events-none absolute inset-0 h-full w-full opacity-[0.22] mix-blend-overlay">
        <filter id="hq-grain">
          <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" stitchTiles="stitch" />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <rect width="100%" height="100%" filter="url(#hq-grain)" />
      </svg>

      {/* Tối dần ra mép để tạo chiều sâu */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_45%,rgba(1,2,10,0.7)_100%)]" />
    </div>
  );
}

export default BrandBackground;
