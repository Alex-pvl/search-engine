import json
import matplotlib.pyplot as plt

json_file_path = './stats.json'

with open(json_file_path, 'r', encoding='utf-8') as file:
    data = json.load(file)

urls = []
urls_count = []
words_count = []
word_locations_count = []
links_count = []
link_words_count = []

for item in data:
    urls.append(item['url'])
    stats = item['statistics']
    urls_count.append(stats.get('urlsCount', 0))
    words_count.append(stats.get('wordsCount', 0))
    word_locations_count.append(stats.get('wordLocationsCount', 0))
    links_count.append(stats.get('linksCount', 0))
    link_words_count.append(stats.get('linkWordsCount', 0))

def plot_statistic(stat_values, stat_name):
    plt.figure(figsize=(10, 6))
    plt.scatter(range(len(urls)), stat_values, color='blue', edgecolor='k')
    plt.ylabel(stat_name)
    plt.title(f'{stat_name}')
    plt.grid(False)
    plt.tight_layout()
    plt.show()

plot_statistic(urls_count, 'Количество URL-ов')
plot_statistic(words_count, 'Количество слов')
plot_statistic(word_locations_count, 'Количество позиций слов')
plot_statistic(links_count, 'Количество ссылок')
plot_statistic(link_words_count, 'Количество слов в ссылках')
