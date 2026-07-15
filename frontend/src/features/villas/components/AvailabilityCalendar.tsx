'use client';

import { useState, useMemo } from 'react';

interface DateRange {
  checkIn: string;
  checkOut: string;
}

interface BookedDate {
  checkIn: string;
  checkOut: string;
}

interface AvailabilityCalendarProps {
  onDateSelect: (range: DateRange) => void;
  bookedDates?: BookedDate[];
  initialCheckIn?: string;
  initialCheckOut?: string;
}

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

function isDateInRange(date: Date, bookedDates: BookedDate[]): boolean {
  return bookedDates.some((bd) => {
    const checkIn = new Date(bd.checkIn);
    const checkOut = new Date(bd.checkOut);
    return date > checkIn && date < checkOut;
  });
}

function isDateBooked(date: Date, bookedDates: BookedDate[]): boolean {
  return bookedDates.some((bd) => {
    const checkIn = new Date(bd.checkIn);
    checkIn.setHours(0, 0, 0, 0);
    const checkOut = new Date(bd.checkOut);
    checkOut.setHours(0, 0, 0, 0);
    return date.getTime() === checkIn.getTime();
  });
}

function formatDateDisplay(date: Date): string {
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
}

export function AvailabilityCalendar({
  onDateSelect,
  bookedDates = [],
  initialCheckIn,
  initialCheckOut,
}: AvailabilityCalendarProps) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const [currentMonth, setCurrentMonth] = useState(today.getMonth());
  const [currentYear, setCurrentYear] = useState(today.getFullYear());
  const [hoverDate, setHoverDate] = useState<Date | null>(null);
  const [selecting, setSelecting] = useState<'checkIn' | 'checkOut'>('checkIn');
  const [checkIn, setCheckIn] = useState<Date | null>(
    initialCheckIn ? new Date(initialCheckIn) : null
  );
  const [checkOut, setCheckOut] = useState<Date | null>(
    initialCheckOut ? new Date(initialCheckOut) : null
  );

  const days = useMemo(() => {
    const firstDay = new Date(currentYear, currentMonth, 1);
    const lastDay = new Date(currentYear, currentMonth + 1, 0);
    const startPadding = firstDay.getDay();
    const totalDays = lastDay.getDate();

    const cells: (Date | null)[] = [];

    // Padding for days before the first of the month
    for (let i = 0; i < startPadding; i++) {
      cells.push(null);
    }

    // Days of the month
    for (let day = 1; day <= totalDays; day++) {
      cells.push(new Date(currentYear, currentMonth, day));
    }

    return cells;
  }, [currentMonth, currentYear]);

  const prevMonth = () => {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear(currentYear - 1);
    } else {
      setCurrentMonth(currentMonth - 1);
    }
  };

  const nextMonth = () => {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear(currentYear + 1);
    } else {
      setCurrentMonth(currentMonth + 1);
    }
  };

  const handleDateClick = (date: Date) => {
    if (date < today) return;
    if (isDateInRange(date, bookedDates) || isDateBooked(date, bookedDates)) return;

    if (selecting === 'checkIn' || !checkIn) {
      setCheckIn(date);
      setCheckOut(null);
      setSelecting('checkOut');
    } else {
      if (date <= checkIn) {
        setCheckIn(date);
        setCheckOut(null);
      } else {
        // Check if any booked date is between checkIn and selected date
        const hasConflict = bookedDates.some((bd) => {
          const bdCheckIn = new Date(bd.checkIn);
          const bdCheckOut = new Date(bd.checkOut);
          return bdCheckIn > checkIn && bdCheckIn < date;
        });

        if (hasConflict) {
          setCheckIn(date);
          setCheckOut(null);
          setSelecting('checkOut');
        } else {
          setCheckOut(date);
          setSelecting('checkIn');
          onDateSelect({
            checkIn: checkIn.toISOString().split('T')[0],
            checkOut: date.toISOString().split('T')[0],
          });
        }
      }
    }
  };

  const isInSelectedRange = (date: Date): boolean => {
    if (!checkIn) return false;
    const end = checkOut || hoverDate;
    if (!end) return false;
    const start = checkIn < end ? checkIn : end;
    const finish = checkIn < end ? end : checkIn;
    return date > start && date < finish;
  };

  const isStartDate = (date: Date): boolean => {
    return checkIn?.toDateString() === date.toDateString();
  };

  const isEndDate = (date: Date): boolean => {
    return checkOut?.toDateString() === date.toDateString();
  };

  const nights = useMemo(() => {
    if (!checkIn || !checkOut) return 0;
    return Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24));
  }, [checkIn, checkOut]);

  const clearDates = () => {
    setCheckIn(null);
    setCheckOut(null);
    setSelecting('checkIn');
  };

  return (
    <div className="rounded-2xl bg-brand-paper p-6 shadow-lg">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h3 className="text-xl font-semibold text-brand-charcoal">Select Dates</h3>
          <p className="mt-1 text-sm text-brand-ink/62">
            {checkIn && checkOut
              ? `${formatDateDisplay(checkIn)} - ${formatDateDisplay(checkOut)} (${nights} nights)`
              : selecting === 'checkIn'
              ? 'Select check-in date'
              : 'Select check-out date'}
          </p>
        </div>
        {(checkIn || checkOut) && (
          <button
            onClick={clearDates}
            className="text-sm text-brand-forest underline underline-offset-2 hover:text-brand-forest-deep"
          >
            Clear
          </button>
        )}
      </div>

      {/* Calendar */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Month Navigation */}
        <div>
          <div className="mb-4 flex items-center justify-between">
            <button
              onClick={prevMonth}
              className="flex h-10 w-10 items-center justify-center rounded-full border border-brand-stone hover:bg-brand-sand"
            >
              ‹
            </button>
            <h4 className="text-lg font-semibold text-brand-charcoal">
              {MONTHS[currentMonth]} {currentYear}
            </h4>
            <button
              onClick={nextMonth}
              className="flex h-10 w-10 items-center justify-center rounded-full border border-brand-stone hover:bg-brand-sand"
            >
              ›
            </button>
          </div>

          <div className="grid grid-cols-7 gap-1">
            {DAYS.map((day) => (
              <div key={day} className="py-2 text-center text-xs font-semibold text-brand-ink/50">
                {day}
              </div>
            ))}
            {days.map((date, index) => {
              if (!date) {
                return <div key={`empty-${index}`} className="h-10" />;
              }

              const isPast = date < today;
              const isBooked = isDateBooked(date, bookedDates) || isDateInRange(date, bookedDates);
              const inRange = isInSelectedRange(date);
              const isStart = isStartDate(date);
              const isEnd = isEndDate(date);
              const isSelected = isStart || isEnd;

              return (
                <button
                  key={date.toISOString()}
                  onClick={() => handleDateClick(date)}
                  onMouseEnter={() => selecting === 'checkOut' && checkIn && date > checkIn && setHoverDate(date)}
                  onMouseLeave={() => setHoverDate(null)}
                  disabled={isPast || isBooked}
                  className={`
                    h-10 w-full rounded-lg text-sm font-medium transition-all
                    ${isPast ? 'text-brand-ink/20 cursor-not-allowed' : ''}
                    ${isBooked ? 'text-brand-coral/50 cursor-not-allowed line-through' : ''}
                    ${isSelected ? 'bg-brand-forest text-brand-white' : ''}
                    ${inRange ? 'bg-brand-sage/30' : ''}
                    ${!isSelected && !isPast && !isBooked ? 'hover:bg-brand-sand' : ''}
                  `}
                >
                  {date.getDate()}
                </button>
              );
            })}
          </div>
        </div>

        {/* Selected Range Summary */}
        <div className="flex flex-col justify-center">
          <div className="space-y-4">
            <div className="flex items-center justify-between rounded-xl bg-brand-sand/50 p-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-brand-ink/50">Check-in</p>
                <p className={`text-lg ${checkIn ? 'text-brand-charcoal' : 'text-brand-ink/30'}`}>
                  {checkIn ? formatDateDisplay(checkIn) : 'Select date'}
                </p>
              </div>
              <span className="text-2xl text-brand-ink/20">→</span>
              <div className="text-right">
                <p className="text-xs font-semibold uppercase tracking-wider text-brand-ink/50">Check-out</p>
                <p className={`text-lg ${checkOut ? 'text-brand-charcoal' : 'text-brand-ink/30'}`}>
                  {checkOut ? formatDateDisplay(checkOut) : 'Select date'}
                </p>
              </div>
            </div>

            {nights > 0 && (
              <div className="rounded-xl bg-brand-forest/10 p-4 text-center">
                <p className="text-3xl font-bold text-brand-forest">{nights}</p>
                <p className="text-sm text-brand-forest/70">nights selected</p>
              </div>
            )}

            <div className="rounded-xl bg-brand-sand/30 p-4">
              <p className="text-xs text-brand-ink/50">Legend</p>
              <div className="mt-2 flex flex-wrap gap-3 text-xs">
                <div className="flex items-center gap-1">
                  <div className="h-4 w-4 rounded bg-brand-forest" />
                  <span>Selected</span>
                </div>
                <div className="flex items-center gap-1">
                  <div className="h-4 w-4 rounded bg-brand-sage/30" />
                  <span>Range</span>
                </div>
                <div className="flex items-center gap-1">
                  <div className="h-4 w-4 rounded bg-brand-coral/30 line-through" />
                  <span>Unavailable</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
