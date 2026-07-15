import type { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
  isLoading?: boolean;
  fullWidth?: boolean;
}

const baseClasses =
  'inline-flex min-h-11 items-center justify-center gap-2 whitespace-nowrap rounded-full font-sans font-medium uppercase tracking-[0.08em] transition duration-200 ease-out focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-gold disabled:pointer-events-none disabled:opacity-50';

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'border border-brand-ink bg-brand-ink text-brand-white hover:border-brand-forest-deep hover:bg-brand-forest-deep active:scale-[0.98]',
  secondary:
    'border border-brand-ink/30 bg-transparent text-brand-ink hover:border-brand-ink hover:bg-brand-ink hover:text-brand-white active:scale-[0.98]',
  tertiary:
    'border border-brand-stone bg-brand-paper text-brand-ink hover:border-brand-sage hover:bg-brand-sand active:scale-[0.98]',
  ghost:
    'border border-transparent bg-transparent text-brand-ink hover:bg-brand-ink/5 active:scale-[0.98]',
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'px-4 py-2 text-[0.72rem]',
  md: 'px-6 py-3 text-xs',
  lg: 'px-8 py-4 text-sm',
};

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  leadingIcon,
  trailingIcon,
  isLoading = false,
  fullWidth = false,
  className = '',
  disabled,
  type = 'button',
  ...props
}: ButtonProps) {
  const isDisabled = disabled || isLoading;

  return (
    <button
      className={`${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${
        fullWidth ? 'w-full' : ''
      } ${className}`}
      disabled={isDisabled}
      aria-busy={isLoading || undefined}
      type={type}
      {...props}
    >
      {isLoading ? (
        <span
          aria-hidden="true"
          className="h-4 w-4 rounded-full border border-current border-t-transparent motion-safe:animate-spin"
        />
      ) : (
        leadingIcon
      )}
      <span>{children}</span>
      {!isLoading && trailingIcon}
    </button>
  );
}

export default Button;
