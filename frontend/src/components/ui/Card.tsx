import { type HTMLAttributes, type ReactNode } from 'react';
import clsx from 'clsx';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  header?: ReactNode;
}

function Card({ className, header, children, ...props }: CardProps) {
  return (
    <div
      className={clsx(
        'bg-card text-card-foreground rounded-xl border border-border shadow-sm transition-shadow duration-200 hover:shadow-md overflow-hidden',
        className,
      )}
      {...props}
    >
      {header && (
        <div className="px-5 py-4 border-b border-border">
          {typeof header === 'string' ? (
            <h3 className="text-lg font-semibold">{header}</h3>
          ) : (
            header
          )}
        </div>
      )}
      <div className="px-5 py-4">{children}</div>
    </div>
  );
}

export { Card, type CardProps };
export default Card;
