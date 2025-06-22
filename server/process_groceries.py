import json
import os
import time
import re

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By

GROCERIES_DIR = "groceries"
SWEDISH_KRONA_RATE = 0.091
HEADERS = {"User-Agent": "Mozilla/5.0"}

def swedish_krona_to_euro(price, number_of_decimals=2):
    return round(price * SWEDISH_KRONA_RATE, number_of_decimals)

def get_price(url, driver):
    
    driver.get(url)
    time.sleep(2)

    try:
        price_element = driver.find_element(By.CSS_SELECTOR, 'span[data-testid="price-container"]')
    except:
        return 0

    return float(price_element.text.strip().replace(",", "."))


if __name__ == "__main__":

    groceries_info = []

    options = Options()
    options.add_argument("--headless")
    driver = webdriver.Chrome(options=options)

    for root, dirs, files in os.walk(GROCERIES_DIR):
        grocery_info = {}

        if dirs == []:
            categories = root.split("/")[1:]

            grocery_info["category"] = categories[0]
            grocery_info["sub_category"] = categories[1]
            grocery_info["name"] = categories[1] if len(categories) == 2 else categories[2]
            grocery_info["image_path"] = grocery_info["name"].replace("-", "_").lower()

            print(grocery_info["name"])

            for file in files:
                file_path = os.path.join(root, file)
                if file.endswith("_Description.txt"):
                    with open(file_path) as f: grocery_info["description"] = f.read()
                elif file.endswith("_Information.txt"):
                    with open(file_path) as f: file_contents = f.read()
                    url = file_contents.split("URL:")[1]
                    price = get_price(url, driver)
                    grocery_info["price"] = swedish_krona_to_euro(price, 2)
                elif not (file.endswith("_Iconic.jpg") or file.endswith("_Description.txt") or file.endswith("_Swedish.txt")) :
                    os.rename(file_path, os.path.join(root, file.lower()))

            groceries_info.append(grocery_info)

    groceries_info.append({
        "category": "Dessert",
        "sub_category": "Dubai Chocolate",
        "name": "Dubai Chocolate",
        "image_path": "dubai_chocolate",
        "price": 104.99,
        "description": "Dubai Chocolate is a luxurious confectionery that blends rich, premium cocoa with exotic Middle Eastern flavors."
    })

    with open(os.path.join(GROCERIES_DIR, "groceries_info.json"), 'w', encoding="UTF-8") as f:
        json.dump(groceries_info, f, ensure_ascii=False, indent=4)

