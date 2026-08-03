import type { HTMLAttributes, ImgHTMLAttributes } from 'react';

type CardTone = 'paper' | 'sand' | 'transparent';
type CardElement = 'article' | 'section' | 'div';

export interface CardProps extends HTMLAttributes<HTMLElement> {
  as?: CardElement;
  tone?: CardTone;
  interactive?: boolean;
  padded?: boolean;
}

export interface CardMediaProps extends Omit<ImgHTMLAttributes<HTMLImageElement>, 'alt'> {
  alt: string;
  ratio?: 'portrait' | 'landscape' | 'square' | 'wide';
}

export interface CardTitleProps extends HTMLAttributes<HTMLHeadingElement> {
  as?: 'h2' | 'h3' | 'h4';
}

const toneClasses: Record<CardTone, string> = {
  paper: 'border-brand-ink/10 bg-brand-paper text-brand-ink',
  sand: 'border-brand-ink/10 bg-brand-sand text-brand-ink',
  transparent: 'border-brand-ink/12 bg-transparent text-brand-ink',
};

const ratioClasses = {
  portrait: 'aspect-[4/5]',
  landscape: 'aspect-[4/3]',
  square: 'aspect-square',
  wide: 'aspect-[16/10]',
};

export function Card({
  as: Element = 'article',
  tone = 'paper',
  interactive = false,
  padded = false,
  className = '',
  children,
  ...props
}: CardProps) {
  return (
    <Element
      className={`group overflow-hidden rounded-[4px] border ${toneClasses[tone]} ${
        padded ? 'p-6' : ''
      } ${
        interactive
          ? 'transition duration-300 ease-out hover:-translate-y-0.5 hover:border-brand-sage focus-within:border-brand-sage focus-within:outline-2 focus-within:outline-offset-4 focus-within:outline-brand-gold'
          : ''
      } ${className}`}
      {...props}
    >
      {children}
    </Element>
  );
}

export function CardMedia({
  alt,
  ratio = 'portrait',
  className = '',
  width,
  height,
  loading = 'lazy',
  decoding = 'async',
  ...props
}: CardMediaProps) {
  return (
    <div className={`overflow-hidden bg-brand-stone ${ratioClasses[ratio]} ${className}`}>
      <img
        className="h-full w-full object-cover transition duration-500 ease-out group-hover:scale-[1.025]"
        width={width}
        height={height}
        loading={loading}
        decoding={decoding}
        alt={alt}
        {...props}
      />
    </div>
  );
}

export function CardContent({ className = '', children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={`space-y-4 p-6 ${className}`} {...props}>
      {children}
    </div>
  );
}

export function CardEyebrow({ className = '', children, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p className={`text-xs font-medium uppercase tracking-[0.16em] text-brand-sage ${className}`} {...props}>
      {children}
    </p>
  );
}

export function CardTitle({
  as: Element = 'h3',
  className = '',
  children,
  ...props
}: CardTitleProps) {
  return (
    <Element className={`font-serif text-3xl leading-tight text-brand-ink ${className}`} {...props}>
      {children}
    </Element>
  );
}

export function CardMeta({ className = '', children, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p className={`text-sm font-medium uppercase tracking-[0.12em] text-brand-ink/58 ${className}`} {...props}>
      {children}
    </p>
  );
}

export function CardDescription({ className = '', children, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p className={`text-base leading-7 text-brand-ink/72 ${className}`} {...props}>
      {children}
    </p>
  );
}

export function CardActions({ className = '', children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={`flex flex-wrap items-center gap-3 pt-2 ${className}`} {...props}>
      {children}
    </div>
  );
}

export function CardBadge({ className = '', children, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={`inline-flex min-h-8 items-center rounded-full border border-brand-ink/12 bg-brand-white/70 px-3 text-xs font-medium uppercase tracking-[0.12em] text-brand-ink ${className}`}
      {...props}
    >
      {children}
    </span>
  );
}

export default Card;
