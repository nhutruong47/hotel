'use client';

import { useMemo, useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Points, PointMaterial } from '@react-three/drei';
import * as THREE from 'three';

function FloatingDust() {
  const ref = useRef<THREE.Points>(null!);
  
  // Generate random positions in a sphere
  const positions = useMemo(() => {
    const count = 800; // number of fireflies/dust particles
    const pos = new Float32Array(count * 3);
    for (let i = 0; i < count; i++) {
      const r = 2.5 * Math.cbrt(Math.random());
      const theta = Math.random() * 2 * Math.PI;
      const phi = Math.acos(2 * Math.random() - 1);
      pos[i * 3] = r * Math.sin(phi) * Math.cos(theta); // x
      pos[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta); // y
      pos[i * 3 + 2] = r * Math.cos(phi); // z
    }
    return pos;
  }, []);
  
  useFrame((state, delta) => {
    if (ref.current) {
      // Slow rotation to simulate drifting wind
      ref.current.rotation.x -= delta * 0.03;
      ref.current.rotation.y -= delta * 0.05;
      ref.current.rotation.z -= delta * 0.01;
    }
  });

  return (
    <group rotation={[0, 0, Math.PI / 4]}>
      <Points ref={ref} positions={positions} stride={3} frustumCulled={false}>
        <PointMaterial
          transparent
          color="#fcf9f2"
          size={0.005}
          sizeAttenuation={true}
          depthWrite={false}
          opacity={0.65}
          blending={THREE.AdditiveBlending}
        />
      </Points>
    </group>
  );
}

export function ParticlesBackground() {
  return (
    <div className="absolute inset-0 z-[2] pointer-events-none">
      <Canvas camera={{ position: [0, 0, 1.2], fov: 60 }}>
        <ambientLight intensity={0.5} />
        <FloatingDust />
      </Canvas>
    </div>
  );
}
