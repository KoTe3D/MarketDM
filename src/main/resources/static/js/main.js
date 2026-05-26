
// Данные ПВЗ
const pickupPoints = [
    { id: 1, name: 'ПВЗ на Тверской', address: 'ул. Тверская, 7', city: 'Москва', coords: [55.757, 37.615] },
    { id: 2, name: 'ПВЗ на Арбате', address: 'ул. Арбат, 20', city: 'Москва', coords: [55.751, 37.590] },
    { id: 3, name: 'ПВЗ в ТЦ "Европейский"', address: 'пл. Киевского вокзала, 2', city: 'Москва', coords: [55.743, 37.565] },
    { id: 4, name: 'ПВЗ на Ленинском', address: 'Ленинский пр-т, 45', city: 'Москва', coords: [55.708, 37.582] },
    { id: 5, name: 'ПВЗ на ВДНХ', address: 'пр-т Мира, 119', city: 'Москва', coords: [55.826, 37.639] }
];

// Элементы DOM
const addressPicker = document.getElementById('addressPicker');
const currentAddressSpan = document.getElementById('currentAddress');
const addressChoiceModal = document.getElementById('addressChoiceModal');
const closeChoiceModal = document.getElementById('closeChoiceModal');
const courierChoice = document.getElementById('courierChoice');
const pickupChoice = document.getElementById('pickupChoice');
const courierPanel = document.getElementById('courierPanel');
const pickupPanel = document.getElementById('pickupPanel');
const openMapModalBtn = document.getElementById('openMapModalBtn');
const mapModal = document.getElementById('mapModal');
const closeMapModal = document.getElementById('closeMapModal');
const selectAddressBtn = document.getElementById('selectAddressBtn');
const searchInputMap = document.getElementById('searchInputMap');
const mapContainer = document.getElementById('mapContainer');
const courierDetails = document.getElementById('courierDetails');
const continueBtn = document.getElementById('continueCourier');
const flatInput = document.getElementById('flat');
const entranceInput = document.getElementById('entrance');
const floorInput = document.getElementById('floor');
const phoneInput = document.getElementById('phone');
const pvzListEl = document.getElementById('pvzList');
const pickupMapContainer = document.getElementById('pickupMapContainer');

// Переменные для карт
let map = null;
let pickupMap = null;
let placemark = null;
let selectedCoords = null;
let selectedAddress = '';
let currentChoice = 'courier';

// === Функции модального окна выбора доставки ===
function openChoiceModal() {
    addressChoiceModal.classList.add('show');
    setActiveChoice('courier');
}
function closeChoiceModalFn() {
    addressChoiceModal.classList.remove('show');
}

function setActiveChoice(choice) {
    currentChoice = choice;
    if (choice === 'courier') {
        courierChoice.classList.add('selected');
        pickupChoice.classList.remove('selected');
        courierPanel.classList.remove('hidden');
        pickupPanel.classList.add('hidden');
    } else {
        pickupChoice.classList.add('selected');
        courierChoice.classList.remove('selected');
        pickupPanel.classList.remove('hidden');
        courierPanel.classList.add('hidden');
        if (!pickupMap) initPickupMap();
        else pickupMap.container.fitToViewport();
    }
}

// === Инициализация карты для курьера ===
function initMap() {
    if (!window.ymaps) {
        console.error('❌ Яндекс.Карты не загружены');
        return;
    }
    ymaps.ready(() => {
        map = new ymaps.Map(mapContainer, { center: [55.76, 37.64], zoom: 10 });

        map.events.add('click', (e) => {
            const coords = e.get('coords');
            if (placemark) map.geoObjects.remove(placemark);
            placemark = new ymaps.Placemark(coords, {}, { preset: 'islands#redDotIcon' });
            map.geoObjects.add(placemark);
            selectedCoords = coords;

            ymaps.geocode(coords).then(res => {
                const firstGeoObject = res.geoObjects.get(0);
                if (firstGeoObject) {
                    selectedAddress = firstGeoObject.getAddressLine();
                    searchInputMap.value = selectedAddress;
                    selectAddressBtn.disabled = false;
                }
            });
        });

        const suggestView = new ymaps.SuggestView('searchInputMap');
        suggestView.events.add('select', (e) => {
            const address = e.get('item').value;
            searchInputMap.value = address;
            ymaps.geocode(address).then(res => {
                const firstGeoObject = res.geoObjects.get(0);
                if (firstGeoObject) {
                    const coords = firstGeoObject.geometry.getCoordinates();
                    selectedCoords = coords;
                    if (placemark) map.geoObjects.remove(placemark);
                    placemark = new ymaps.Placemark(coords, {}, { preset: 'islands#redDotIcon' });
                    map.geoObjects.add(placemark);
                    map.setCenter(coords, 15);
                    selectedAddress = firstGeoObject.getAddressLine();
                    selectAddressBtn.disabled = false;
                }
            });
        });

        searchInputMap.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                const address = searchInputMap.value.trim();
                if (!address) return;
                ymaps.geocode(address).then(res => {
                    const firstGeoObject = res.geoObjects.get(0);
                    if (firstGeoObject) {
                        const coords = firstGeoObject.geometry.getCoordinates();
                        selectedCoords = coords;
                        if (placemark) map.geoObjects.remove(placemark);
                        placemark = new ymaps.Placemark(coords, {}, { preset: 'islands#redDotIcon' });
                        map.geoObjects.add(placemark);
                        map.setCenter(coords, 15);
                        selectedAddress = firstGeoObject.getAddressLine();
                        selectAddressBtn.disabled = false;
                    } else {
                        alert('Адрес не найден. Попробуйте уточнить запрос.');
                    }
                }).catch(err => {
                    console.error('Ошибка геокодирования:', err);
                    alert('Ошибка при поиске адреса.');
                });
            }
        });
    });
}

// === Пункты выдачи ===
function initPickupMap() {
    if (!window.ymaps) { console.error('❌ Яндекс.Карты не загружены'); return; }
    ymaps.ready(() => {
        pickupMap = new ymaps.Map(pickupMapContainer, { center: [55.76, 37.64], zoom: 10 });
        pickupPoints.forEach(point => {
            const placemark = new ymaps.Placemark(point.coords, {
                balloonContent: `<strong>${point.name}</strong><br>${point.address}`
            }, { preset: 'islands#redDotIcon' });
            placemark.events.add('click', () => selectPickupPoint(point));
            pickupMap.geoObjects.add(placemark);
        });
    });
    renderPvzList();
}

function renderPvzList() {
    pvzListEl.innerHTML = '';
    pickupPoints.forEach(point => {
        const div = document.createElement('div');
        div.className = 'pvz-item';
        div.textContent = `${point.name} — ${point.address}`;
        div.addEventListener('click', () => selectPickupPoint(point));
        pvzListEl.appendChild(div);
    });
}

function selectPickupPoint(point) {
    currentAddressSpan.textContent = point.city + ', ' + point.address;
    localStorage.setItem('deliveryAddress', point.city + ', ' + point.address);
    localStorage.removeItem('deliveryDetails');
    closeChoiceModalFn();
}

// === Загрузка профиля ===
async function loadUserInfo() {
    const token = localStorage.getItem('jwt');
    const userInfoDiv = document.getElementById('userInfo');
    if (!token) {
        userInfoDiv.innerHTML = '<a href="/login">Вход</a><a href="/register">Регистрация</a>';
        return;
    }
    try {
        const response = await fetch('/api/users/profile', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (response.ok) {
            const user = await response.json();
            const isAdmin = user.roles && user.roles.includes('ROLE_ADMIN');
            let userHtml = '<a href="/cart-icon2.html" class="icon-btn"><img src="/images/cart-icon2.png" alt="Корзина" class="cart-icon2"><span class="icon-label">Корзина</span></a><a href="/profile-icon2.html" class="icon-btn"><img src="/images/profile-icon2.png" alt="Профиль" class="profile-icon2"><span class="icon-label">Профиль</span></a>';
            if (isAdmin) userHtml += '<span class="admin-badge">ADMIN</span>';
            userHtml += '<button class="logout-btn" id="logoutBtn">Выйти</button>';
            userInfoDiv.innerHTML = userHtml;
            document.getElementById('logoutBtn').addEventListener('click', () => {
                localStorage.removeItem('jwt');
                window.location.reload();
            });
        } else {
            localStorage.removeItem('jwt');
            userInfoDiv.innerHTML = '<a href="/login">Вход</a><a href="/register">Регистрация</a>';
        }
    } catch (err) {
        console.error('Ошибка загрузки профиля', err);
        userInfoDiv.innerHTML = '<a href="/login">Вход</a><a href="/register">Регистрация</a>';
    }
}

// === Модальное окно соцсетей ===
function openSocialModal(socialName) {
    const socialModal = document.getElementById('socialModal');
    const socialModalTitle = document.getElementById('socialModalTitle');
    const socialModalMessage = document.getElementById('socialModalMessage');
    socialModalTitle.textContent = 'Скоро появится';
    socialModalMessage.textContent = `Здесь будет ваш ${socialName}`;
    socialModal.classList.add('show');
}
function closeSocialModalFn() {
    const socialModal = document.getElementById('socialModal');
    socialModal.classList.remove('show');
}

// === Каталог ===
const subcategoriesData = {
    electronics: ['Смартфоны', 'Ноутбуки', 'Наушники', 'Умные часы', 'Планшеты'],
    home: ['Пылесосы', 'Кухонная техника', 'Текстиль', 'Освещение', 'Мебель'],
    clothing: ['Мужская одежда', 'Женская одежда', 'Детская одежда', 'Обувь', 'Аксессуары'],
    sports: ['Тренажёры', 'Спортивная одежда', 'Мячи', 'Велоспорт', 'Туризм'],
    kids: ['Игрушки', 'Коляски', 'Детская мебель', 'Подгузники', 'Развивающие игры']
};
const categories = [
    { id: 'electronics', name: 'Электроника', icon: '📱' },
    { id: 'home', name: 'Товары для дома', icon: '🏠' },
    { id: 'clothing', name: 'Одежда', icon: '👕' },
    { id: 'sports', name: 'Спорттовары', icon: '⚽' },
    { id: 'kids', name: 'Детские товары', icon: '🧸' }
];

function showSubcategories(categoryId) {
    const subs = subcategoriesData[categoryId] || [];
    const subcategoryList = document.getElementById('subcategoryList');
    const subcategoriesPanel = document.getElementById('subcategoriesPanel');
    subcategoryList.innerHTML = '';
    subs.forEach(sub => {
        const li = document.createElement('li');
        li.className = 'subcategory-item';
        li.textContent = sub;
        li.addEventListener('click', () => {
            document.getElementById('categoryModal').classList.remove('show');
            window.location.href = `/subcategory.html?cat=${categoryId}&sub=${encodeURIComponent(sub)}`;
        });
        subcategoryList.appendChild(li);
    });
    subcategoriesPanel.classList.add('visible');
}

function renderCategoryMenu() {
    const categoryList = document.getElementById('categoryList');
    categoryList.innerHTML = '';
    categories.forEach(cat => {
        const li = document.createElement('li');
        li.className = 'category-item';
        li.dataset.id = cat.id;
        li.innerHTML = `${cat.icon} ${cat.name}`;
        li.addEventListener('mouseenter', () => showSubcategories(cat.id));
        li.addEventListener('click', () => {
            document.getElementById('categoryModal').classList.remove('show');
            window.location.href = `/category.html?id=${cat.id}`;
        });
        categoryList.appendChild(li);
    });
}

// === Чат поддержки ===
let currentTicketId = null;

function addMessage(sender, text, fromOperator) {
    const div = document.createElement('div');
    div.style.padding = '8px 12px';
    div.style.margin = '6px 0';
    div.style.alignSelf = fromOperator ? 'flex-start' : 'flex-end';
    div.style.backgroundColor = fromOperator ? '#e6f7ff' : '#00cdfd';
    div.style.color = fromOperator ? '#002951' : 'white';
    div.style.borderRadius = '12px';
    div.style.maxWidth = '80%';
    div.style.wordBreak = 'break-word';
    const strong = document.createElement('strong');
    strong.textContent = sender;
    const br = document.createElement('br');
    const messageText = document.createElement('span');
    messageText.textContent = text;
    div.appendChild(strong);
    div.appendChild(br);
    div.appendChild(messageText);
    document.getElementById('chatMessages')?.appendChild(div);
    scrollChat();
}

function scrollChat() {
    const chat = document.getElementById('chatMessages');
    if (!chat) return;
    chat.scrollTop = chat.scrollHeight;
}

function loadMessages() {
    if (!currentTicketId) return;
    fetch(`/api/support/tickets/${currentTicketId}/messages`)
        .then(res => res.json())
        .then(messages => {
            const container = document.getElementById('chatMessages');
            if (!container) return;
            container.innerHTML = '';
            messages.forEach(m => addMessage(m.senderName, m.message, m.fromOperator));
            scrollChat();
        })
        .catch(() => setTimeout(loadMessages, 5000));
}

// === Инициализация при загрузке страницы ===
document.addEventListener('DOMContentLoaded', () => {
    // Привязка событий
    if (addressPicker) addressPicker.addEventListener('click', openChoiceModal);
    if (closeChoiceModal) closeChoiceModal.addEventListener('click', closeChoiceModalFn);
    if (courierChoice) courierChoice.addEventListener('click', () => setActiveChoice('courier'));
    if (pickupChoice) pickupChoice.addEventListener('click', () => setActiveChoice('pickup'));
    if (openMapModalBtn) openMapModalBtn.addEventListener('click', () => {
        document.getElementById('mapModal').classList.add('show');
        if (!map) initMap();
        else map.container.fitToViewport();
    });
    if (document.getElementById('closeMapModal')) {
        document.getElementById('closeMapModal').addEventListener('click', () => {
            document.getElementById('mapModal').classList.remove('show');
        });
    }
    if (selectAddressBtn) selectAddressBtn.addEventListener('click', () => {
        if (!selectedAddress) return;
        document.getElementById('mapModal').classList.remove('show');
        document.getElementById('courierDetails').classList.remove('hidden');
    });
    if (continueBtn) continueBtn.addEventListener('click', () => {
        const flat = flatInput?.value.trim();
        const entrance = entranceInput?.value.trim();
        const floor = floorInput?.value.trim();
        const phone = phoneInput?.value.trim();
        if (!selectedAddress) { alert('Сначала выберите адрес на карте'); return; }
        if (!flat || !phone) { alert('Заполните квартиру и телефон'); return; }
        currentAddressSpan.textContent = selectedAddress;
        localStorage.setItem('deliveryAddress', selectedAddress);
        localStorage.setItem('deliveryDetails', JSON.stringify({ flat, entrance, floor, phone }));
        closeChoiceModalFn();
    });

    // Поиск
    const searchBtn = document.getElementById('searchBtn');
    const searchInput = document.getElementById('searchInput');
    if (searchBtn) searchBtn.addEventListener('click', () => {
        const query = searchInput?.value.trim();
        if (query) window.location.href = `/search.html?q=${encodeURIComponent(query)}`;
    });
    if (searchInput) searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && searchBtn) searchBtn.click();
    });

    // Каталог
    const catalogBtn = document.getElementById('catalogBtn');
    const catalogModal = document.getElementById('categoryModal');
    const closeCatalogModal = document.getElementById('closeModalBtn');
    if (catalogBtn) catalogBtn.addEventListener('click', () => {
        catalogModal?.classList.add('show');
        showSubcategories('electronics');
    });
    if (closeCatalogModal) closeCatalogModal.addEventListener('click', () => catalogModal?.classList.remove('show'));
    if (catalogModal) catalogModal.addEventListener('click', (e) => {
        if (e.target === catalogModal) catalogModal.classList.remove('show');
    });

    // Соцсети
    const closeSocialModal = document.getElementById('closeSocialModal');
    const socialModalBtn = document.getElementById('socialModalBtn');
    if (closeSocialModal) closeSocialModal.addEventListener('click', closeSocialModalFn);
    if (socialModalBtn) socialModalBtn.addEventListener('click', closeSocialModalFn);
    const socialModal = document.getElementById('socialModal');
    if (socialModal) socialModal.addEventListener('click', (e) => {
        if (e.target === socialModal) closeSocialModalFn();
    });

    // Чат поддержки
    const floatingSupportBtn = document.getElementById('floatingSupportBtn');
    const supportModal = document.getElementById('supportModal');
    const chatPanel = document.getElementById('chatPanel');
    const openChatBtn = document.getElementById('openChatBtn');
    const closeChat = document.getElementById('closeChat');
    const createTicketBtn = document.getElementById('createTicketBtn');
    const sendMessageBtn = document.getElementById('sendMessageBtn');

    if (document.getElementById('supportLink')) {
        document.getElementById('supportLink').addEventListener('click', (e) => {
            e.preventDefault();
            if (supportModal) supportModal.style.display = 'flex';
        });
    }
    if (floatingSupportBtn) floatingSupportBtn.addEventListener('click', () => {
        if (supportModal) supportModal.style.display = 'none';
        if (chatPanel) chatPanel.style.right = '24px';
        if (floatingSupportBtn) floatingSupportBtn.style.display = 'none';
    });
    if (supportModal) supportModal.addEventListener('click', (e) => {
        if (e.target === e.currentTarget) e.target.style.display = 'none';
    });
    if (openChatBtn) openChatBtn.addEventListener('click', () => {
        if (supportModal) supportModal.style.display = 'none';
        if (chatPanel) chatPanel.style.right = '24px';
        if (floatingSupportBtn) floatingSupportBtn.style.display = 'none';
    });
    if (closeChat) closeChat.addEventListener('click', () => {
        if (chatPanel) chatPanel.style.right = '-420px';
        if (floatingSupportBtn) floatingSupportBtn.style.display = 'block';
        currentTicketId = null;
    });
    if (createTicketBtn) createTicketBtn.addEventListener('click', async () => {
        const subject = document.getElementById('subjectInput')?.value;
        const desc = document.getElementById('descInput')?.value;
        if (!subject || !desc) { alert("Заполните тему и описание"); return; }
        try {
            const res = await fetch('/api/support/tickets', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ subject, description: desc })
            });
            const ticket = await res.json();
            currentTicketId = ticket.id;
            const ticketForm = document.getElementById('ticketForm');
            const messageInputBox = document.getElementById('messageInputBox');
            if (ticketForm) ticketForm.style.display = 'none';
            if (messageInputBox) messageInputBox.style.display = 'block';
            addMessage("Система", "Тикет создан. Ожидайте ответа оператора.", true);
            loadMessages();
        } catch (err) {
            alert("Ошибка подключения к серверу");
            console.error(err);
        }
    });
    if (sendMessageBtn) sendMessageBtn.addEventListener('click', async () => {
        const input = document.getElementById('messageInput');
        const msg = input?.value.trim();
        if (!currentTicketId || !msg) return;
        try {
            await fetch(`/api/support/tickets/${currentTicketId}/messages`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: msg })
            });
            addMessage("Вы", msg, false);
            input.value = '';
            loadMessages();
        } catch (err) {
            addMessage("Система", "Не удалось отправить. Проверьте соединение.", true);
        }
    });

    // Запуск
    loadUserInfo();
    renderCategoryMenu();

    // Автообновление чата
    setInterval(loadMessages, 3000);

    // Сохранённый адрес
    const savedAddress = localStorage.getItem('deliveryAddress');
    if (savedAddress && currentAddressSpan) currentAddressSpan.textContent = savedAddress;
});