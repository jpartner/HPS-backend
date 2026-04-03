'use client';

import { type ReactNode, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import clsx from 'clsx';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeStyles = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
};

function Modal({ open, onClose, title, children, size = 'md', className }: ModalProps) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (open) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, handleKeyDown]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? 'modal-title' : undefined}
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-overlay"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Content */}
      <div
        className={clsx(
          'relative w-full bg-card rounded-xl shadow-lg border border-border animate-slide-up',
          sizeStyles[size],
          className,
        )}
      >
        {/* Header */}
        {title && (
          <div className="flex items-center justify-between px-5 py-4 border-b border-border">
            <h2 id="modal-title" className="text-lg font-semibold text-card-foreground">
              {title}
            </h2>
            <button
              onClick={onClose}
              className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors cursor-pointer"
              aria-label="Close"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        )}

        {/* Close button when no title */}
        {!title && (
          <button
            onClick={onClose}
            className="absolute top-3 right-3 p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors cursor-pointer"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        )}

        {/* Body */}
        <div className="px-5 py-4">{children}</div>
      </div>
    </div>
  );
}

export { Modal, type ModalProps };
export default Modal;
