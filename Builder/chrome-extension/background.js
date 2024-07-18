chrome.action.onClicked.addListener((tab) => {
    chrome.tabs.create({ url: 'https://librera.mobi/online-book-reader/' });
  });