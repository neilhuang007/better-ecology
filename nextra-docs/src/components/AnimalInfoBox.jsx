export function AnimalInfoBox({
  name,
  color = '#4CAF50',
  health,
  speed,
  food,
  activeTime,
  temperament,
  mobType,
  drops
}) {
  const infoItems = [
    { label: 'Health', value: health },
    { label: 'Speed', value: speed },
    { label: 'Food', value: food },
    { label: 'Active Time', value: activeTime },
    { label: 'Temperament', value: temperament },
    { label: 'Mob Type', value: mobType },
    { label: 'Drops', value: drops }
  ];

  const filteredItems = infoItems.filter(item => item.value);

  return (
    <div className="animal-info-box" style={{ '--accent-color': color }}>
      <h3 className="animal-info-title">{name}</h3>
      <div className="animal-info-grid">
        {filteredItems.map((item, index) => (
          <div key={index} className="animal-info-item">
            <span className="animal-info-label">{item.label}:</span>
            <span className="animal-info-value">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
