'use client';

import { type InputHTMLAttributes, forwardRef } from 'react';
import clsx from 'clsx';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, helperText, id, ...props }, ref) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-medium text-foreground mb-1.5"
          >
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={clsx(
            'w-full rounded-lg border bg-white px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted-foreground transition-colors duration-150',
            'focus:outline-none focus:ring-2 focus:ring-input-focus/30 focus:border-input-focus',
            error
              ? 'border-danger focus:ring-danger/30 focus:border-danger'
              : 'border-input-border hover:border-muted-foreground/50',
            'disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-muted',
            className,
          )}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={
            error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined
          }
          {...props}
        />
        {error && (
          <p id={`${inputId}-error`} className="mt-1.5 text-sm text-danger">
            {error}
          </p>
        )}
        {!error && helperText && (
          <p id={`${inputId}-helper`} className="mt-1.5 text-sm text-muted-foreground">
            {helperText}
          </p>
        )}
      </div>
    );
  },
);

Input.displayName = 'Input';

export { Input, type InputProps };
export default Input;
