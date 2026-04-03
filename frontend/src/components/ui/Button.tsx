'use client';

import { type ButtonHTMLAttributes, forwardRef } from 'react';
import clsx from 'clsx';

const variantStyles = {
  primary:
    'bg-primary text-primary-foreground hover:bg-primary-hover shadow-sm active:scale-[0.98]',
  secondary:
    'bg-secondary text-secondary-foreground hover:bg-secondary-hover shadow-sm active:scale-[0.98]',
  outline:
    'border border-border bg-transparent text-foreground hover:bg-muted active:scale-[0.98]',
  ghost:
    'bg-transparent text-foreground hover:bg-muted',
  danger:
    'bg-danger text-danger-foreground hover:bg-red-600 shadow-sm active:scale-[0.98]',
};

const sizeStyles = {
  sm: 'px-3 py-1.5 text-sm rounded-md gap-1.5',
  md: 'px-4 py-2 text-sm rounded-lg gap-2',
  lg: 'px-6 py-3 text-base rounded-lg gap-2.5',
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: keyof typeof variantStyles;
  size?: keyof typeof sizeStyles;
  loading?: boolean;
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading = false, disabled, children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={clsx(
          'inline-flex items-center justify-center font-medium transition-all duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:opacity-50 disabled:pointer-events-none cursor-pointer',
          variantStyles[variant],
          sizeStyles[size],
          className,
        )}
        disabled={disabled || loading}
        {...props}
      >
        {loading && (
          <svg
            className="animate-spin h-4 w-4 shrink-0"
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
            />
          </svg>
        )}
        {children}
      </button>
    );
  },
);

Button.displayName = 'Button';

export { Button, type ButtonProps };
export default Button;
