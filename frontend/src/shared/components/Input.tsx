import { useId, type InputHTMLAttributes, type ReactNode } from 'react';

type InputTone = 'default' | 'success' | 'error';

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label: ReactNode;
  helperText?: ReactNode;
  errorText?: ReactNode;
  successText?: ReactNode;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
  inputSize?: 'sm' | 'md' | 'lg';
  wrapperClassName?: string;
  inputClassName?: string;
}

const sizeClasses = {
  sm: 'min-h-11 px-4 text-sm',
  md: 'min-h-12 px-4 text-base',
  lg: 'min-h-14 px-5 text-base',
};

const toneClasses: Record<InputTone, string> = {
  default:
    'border-brand-ink/20 bg-brand-white/70 text-brand-ink hover:border-brand-sage focus:border-brand-forest',
  success:
    'border-brand-forest bg-brand-white/70 text-brand-ink focus:border-brand-forest',
  error:
    'border-brand-coral bg-brand-white/80 text-brand-ink focus:border-brand-coral',
};

export function Input({
  id,
  label,
  helperText,
  errorText,
  successText,
  leadingIcon,
  trailingIcon,
  inputSize = 'md',
  wrapperClassName = '',
  inputClassName = '',
  className = '',
  disabled,
  required,
  ...props
}: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const helperId = `${inputId}-helper`;
  const feedbackId = `${inputId}-feedback`;
  const tone: InputTone = errorText ? 'error' : successText ? 'success' : 'default';
  const describedBy = [helperText ? helperId : undefined, errorText || successText ? feedbackId : undefined]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={`flex w-full flex-col gap-2 ${wrapperClassName}`}>
      <label htmlFor={inputId} className="text-sm font-medium text-brand-ink">
        {label}
        {required ? <span className="ml-1 text-brand-coral" aria-hidden="true">*</span> : null}
      </label>

      <div className="relative">
        {leadingIcon ? (
          <span
            aria-hidden="true"
            className="pointer-events-none absolute left-4 top-1/2 flex -translate-y-1/2 text-brand-sage"
          >
            {leadingIcon}
          </span>
        ) : null}
        <input
          id={inputId}
          className={`w-full rounded-[4px] border font-sans outline-none transition duration-200 placeholder:text-brand-ink/42 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-gold disabled:cursor-not-allowed disabled:bg-brand-stone/45 disabled:text-brand-ink/55 ${
            leadingIcon ? 'pl-11' : ''
          } ${trailingIcon ? 'pr-11' : ''} ${sizeClasses[inputSize]} ${toneClasses[tone]} ${inputClassName} ${className}`}
          disabled={disabled}
          required={required}
          aria-invalid={errorText ? true : undefined}
          aria-describedby={describedBy || undefined}
          {...props}
        />
        {trailingIcon ? (
          <span
            aria-hidden="true"
            className="pointer-events-none absolute right-4 top-1/2 flex -translate-y-1/2 text-brand-sage"
          >
            {trailingIcon}
          </span>
        ) : null}
      </div>

      {helperText ? (
        <p id={helperId} className="text-sm leading-6 text-brand-ink/68">
          {helperText}
        </p>
      ) : null}
      {errorText || successText ? (
        <p
          id={feedbackId}
          className={`text-sm leading-6 ${errorText ? 'text-brand-coral' : 'text-brand-forest'}`}
        >
          {errorText ?? successText}
        </p>
      ) : null}
    </div>
  );
}

export default Input;
