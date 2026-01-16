export function AnimalInfoBox({
  name,
  color = '#4CAF50',
  health,
  speed,
  food,
  activeTime,
  temperament,
  mobType,
  drops,
  habitat,
  diet,
  specialAbilities
}) {
  const infoItems = [
    { label: 'Health', value: health },
    { label: 'Speed', value: speed },
    { label: 'Food', value: food },
    { label: 'Diet', value: diet },
    { label: 'Active Time', value: activeTime },
    { label: 'Temperament', value: temperament },
    { label: 'Mob Type', value: mobType },
    { label: 'Habitat', value: habitat },
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
        {specialAbilities && specialAbilities.length > 0 && (
          <div className="animal-info-item animal-info-abilities">
            <span className="animal-info-label">Special Abilities:</span>
            <span className="animal-info-value">
              {specialAbilities.join(', ')}
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
