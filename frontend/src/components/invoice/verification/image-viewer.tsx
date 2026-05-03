import React from 'react';
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch';
import { ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import Image from 'next/image';

interface ImageViewerProps {
  url: string;
  alt: string;
}

export const ImageViewer: React.FC<ImageViewerProps> = ({ url, alt }) => {
  return (
    <div className="h-full w-full flex flex-col bg-gray-100 dark:bg-gray-900 overflow-hidden relative">
      <TransformWrapper
        initialScale={1}
        minScale={0.5}
        maxScale={4}
        centerOnInit
      >
        {({ zoomIn, zoomOut, resetTransform }) => (
          <>
            <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 flex gap-2 bg-white/90 dark:bg-black/90 p-1.5 rounded-full shadow-lg border border-gray-200 dark:border-gray-800">
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 rounded-full"
                onClick={() => zoomIn()}
                title="Yakınlaştır"
              >
                <ZoomIn className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 rounded-full"
                onClick={() => zoomOut()}
                title="Uzaklaştır"
              >
                <ZoomOut className="h-4 w-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 rounded-full"
                onClick={() => resetTransform()}
                title="Sıfırla"
              >
                <RotateCcw className="h-4 w-4" />
              </Button>
            </div>

            <div className="flex-1 w-full h-full cursor-grab active:cursor-grabbing">
              <TransformComponent
                wrapperStyle={{ width: "100%", height: "100%" }}
                contentStyle={{ width: "100%", height: "100%", display: "flex", justifyContent: "center", alignItems: "center" }}
              >
                <div className="relative w-full h-full">
                   <Image
                      src={url}
                      alt={alt}
                      fill
                      className="object-contain shadow-lg"
                      unoptimized 
                    />
                </div>
              </TransformComponent>
            </div>
          </>
        )}
      </TransformWrapper>
    </div>
  );
};
