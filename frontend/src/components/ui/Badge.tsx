import { type HTMLAttributes } from 'react';
import clsx from 'clsx';

const variantStyles = {
  default: 'bg-muted text-secondary-foreground',
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  warning: 'bg-amber-50 text-amber-700 border-amber-200',
  error: 'bg-red-50 text-red-700 border-red-200',
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: keyof typeof variantStyles;
}

function Badge({ className, variant = 'default', children, ...props }: BadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full border border-transparent px-2.5 py-0.5 text-xs font-medium transition-colors',
        variantStyles[variant],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}

export { Badge, type BadgeProps };
export default Badge;
