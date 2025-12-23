import { describe, expect, it } from 'vitest';
import { describeSchedule, describeTarget, streakLabel } from './schedule';

describe('describeSchedule', () => {
  it('describes daily and weekly schedules', () => {
    expect(describeSchedule({ type: 'daily' })).toBe('Every day');
    expect(describeSchedule({ type: 'weekly', timesPerWeek: 1 })).toBe('Once a week');
    expect(describeSchedule({ type: 'weekly', timesPerWeek: 3 })).toBe('3 times a week');
  });

  it('names weekday sets', () => {
    expect(describeSchedule({ type: 'weekdays', days: [5, 1, 3] })).toBe('Mon, Wed, Fri');
    expect(describeSchedule({ type: 'weekdays', days: [1, 2, 3, 4, 5] })).toBe('Weekdays');
    expect(describeSchedule({ type: 'weekdays', days: [6, 7] })).toBe('Weekends');
    expect(describeSchedule({ type: 'weekdays', days: [1, 2, 3, 4, 5, 6, 7] })).toBe('Every day');
  });
});

describe('describeTarget', () => {
  it('includes the unit when there is one', () => {
    expect(describeTarget({ target: 8, unit: 'glasses' })).toBe('8 glasses');
    expect(describeTarget({ target: 30, unit: null })).toBe('30');
    expect(describeTarget({ target: null, unit: null })).toBeNull();
  });
});

describe('streakLabel', () => {
  it('pluralises', () => {
    expect(streakLabel(1, 'days')).toBe('1 day');
    expect(streakLabel(4, 'days')).toBe('4 days');
    expect(streakLabel(1, 'weeks')).toBe('1 week');
    expect(streakLabel(0, 'weeks')).toBe('0 weeks');
  });
});
