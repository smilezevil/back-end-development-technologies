let currentHotels = []; // Зберігаємо список тут, щоб було легко редагувати

document.addEventListener('DOMContentLoaded', () => {
    fetchHotels();
    setupModal();
});

// === ОТРИМАННЯ ДАНИХ (READ) ===
async function fetchHotels() {
    const listContainer = document.getElementById('hotels-list');

    try {
        const response = await fetch('/api/hotels');
        if (!response.ok) throw new Error('Помилка сервера');

        currentHotels = await response.json();
        listContainer.innerHTML = '';

        if (currentHotels.length === 0) {
            listContainer.innerHTML = '<p>Готелів поки немає.</p>';
            return;
        }

        currentHotels.forEach(hotel => {
            const card = document.createElement('div');
            card.className = 'hotel-card';
            card.innerHTML = `
                <div>
                    <h3>${hotel.name}</h3>
                    <p style="margin-top: 4px; margin-bottom: 8px;">📍 ${hotel.city}, ${hotel.address}</p>
                    <p>${hotel.description}</p>
                </div>
                <div class="card-actions">
                    <button class="edit-btn" onclick="openEditModal(${hotel.id})">Редагувати</button>
                    <button class="delete-btn" onclick="deleteHotel(${hotel.id})">Видалити</button>
                </div>
            `;
            listContainer.appendChild(card);
        });
    } catch (error) {
        listContainer.innerHTML = `<p style="color: red;">Помилка: ${error.message}</p>`;
    }
}

// === ЛОГІКА МОДАЛКИ (CREATE / UPDATE) ===
function setupModal() {
    const modal = document.getElementById('hotel-modal');
    const form = document.getElementById('hotel-form');

    // Відкрити для СТВОРЕННЯ
    document.getElementById('open-modal-btn').addEventListener('click', () => {
        document.getElementById('h-id').value = ''; // Очищаємо ID
        document.getElementById('modal-title').textContent = 'Новий готель';
        form.reset();
        modal.classList.remove('hidden');
    });

    // Закрити
    const closeModal = () => {
        modal.classList.add('hidden');
        form.reset();
    };
    document.getElementById('close-modal').addEventListener('click', closeModal);
    document.getElementById('cancel-btn').addEventListener('click', closeModal);

    // Відправка форми
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const id = document.getElementById('h-id').value;
        const hotelData = {
            name: document.getElementById('h-name').value,
            city: document.getElementById('h-city').value,
            address: document.getElementById('h-address').value,
            description: document.getElementById('h-desc').value
        };

        // Якщо є ID — це PUT (Update), якщо немає — POST (Create)
        const method = id ? 'PUT' : 'POST';
        const url = id ? `/api/hotels/${id}` : '/api/hotels';

        try {
            const response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(hotelData)
            });

            if (response.ok) {
                closeModal();
                fetchHotels(); // Оновлюємо список
            } else {
                alert('Не вдалося зберегти готель');
            }
        } catch (error) {
            alert('Помилка: ' + error.message);
        }
    });
}

// Функція відкриття модалки для РЕДАГУВАННЯ
window.openEditModal = function(id) {
    const hotel = currentHotels.find(h => h.id === id);
    if (!hotel) return;

    document.getElementById('h-id').value = hotel.id;
    document.getElementById('h-name').value = hotel.name;
    document.getElementById('h-city').value = hotel.city;
    document.getElementById('h-address').value = hotel.address;
    document.getElementById('h-desc').value = hotel.description;

    document.getElementById('modal-title').textContent = 'Редагувати готель';
    document.getElementById('hotel-modal').classList.remove('hidden');
};

// === ВИДАЛЕННЯ ГОТЕЛЮ (DELETE) ===
window.deleteHotel = async function(id) {
    if (!confirm('Ти точно хочеш видалити цей готель?')) return;

    try {
        const response = await fetch(`/api/hotels/${id}`, { method: 'DELETE' });
        if (response.ok) {
            fetchHotels();
        } else {
            alert('Не вдалося видалити готель');
        }
    } catch (error) {
        alert('Помилка: ' + error.message);
    }
}