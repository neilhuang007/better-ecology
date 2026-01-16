import React from 'react';

const hintConfig = {
  didYouKnow: {
    label: 'Did You Know?',
    icon: '💡',
    className: 'hint-box-did-you-know'
  },
  hint: {
    label: 'Hint',
    icon: '🔍',
    className: 'hint-box-hint'
  },
  tryThis: {
    label: 'Try This',
    icon: '🎯',
    className: 'hint-box-try-this'
  },
  tip: {
    label: 'Tip',
    icon: '⭐',
    className: 'hint-box-tip'
  },
  funFact: {
    label: 'Fun Fact',
    icon: '🎮',
    className: 'hint-box-fun-fact'
  },
  warning: {
    label: 'Warning',
    icon: '⚠️',
    className: 'hint-box-warning'
  },
  info: {
    label: 'Info',
    icon: 'ℹ️',
    className: 'hint-box-info'
  },
  observation: {
    label: 'Observation',
    icon: '👁️',
    className: 'hint-box-observation'
  },
  mechanics: {
    label: 'Game Mechanics',
    icon: '⚙️',
    className: 'hint-box-mechanics'
  },
  experiment: {
    label: 'Experiment',
    icon: '🧪',
    className: 'hint-box-experiment'
  }
};

export function HintBox({ type = 'hint', children }) {
  const config = hintConfig[type] || hintConfig.hint;

  return (
    <div className={`hint-box ${config.className}`}>
      <div className="hint-box-header">
        <span className="hint-box-icon">{config.icon}</span>
        <span className="hint-box-label">{config.label}</span>
      </div>
      <div className="hint-box-content">
        {children}
      </div>
    </div>
  );
}
