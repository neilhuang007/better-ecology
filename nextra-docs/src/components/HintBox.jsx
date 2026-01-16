const hintConfig = {
  didYouKnow: {
    label: 'Did You Know',
    className: 'hint-box-did-you-know'
  },
  hint: {
    label: 'Hint',
    className: 'hint-box-hint'
  },
  tryThis: {
    label: 'Try This',
    className: 'hint-box-try-this'
  },
  tip: {
    label: 'Tip',
    className: 'hint-box-tip'
  },
  funFact: {
    label: 'Fun Fact',
    className: 'hint-box-fun-fact'
  },
  warning: {
    label: 'Warning',
    className: 'hint-box-warning'
  },
  info: {
    label: 'Info',
    className: 'hint-box-info'
  },
  observation: {
    label: 'Observation',
    className: 'hint-box-observation'
  },
  mechanics: {
    label: 'Game Mechanics',
    className: 'hint-box-mechanics'
  },
  experiment: {
    label: 'Experiment',
    className: 'hint-box-experiment'
  },
  note: {
    label: 'Note',
    className: 'hint-box-info'
  },
  success: {
    label: 'Success',
    className: 'hint-box-try-this'
  }
};

export function HintBox({ type = 'hint', children }) {
  const config = hintConfig[type] || hintConfig.hint;

  return (
    <div className={`hint-box ${config.className}`}>
      <div className="hint-box-header">
        <span className="hint-box-label">{config.label}</span>
      </div>
      <div className="hint-box-content">
        {children}
      </div>
    </div>
  );
}
