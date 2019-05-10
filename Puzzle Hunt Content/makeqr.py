# makeqr.py
# Tim Player and Richie Harris
# 6 May 2019
# To run this code, first type
# pip install qrcode[pil]
# if you want to use a Python environment manager like Anaconda,
# remember to first activate the environment you want to install qrcode into.

import qrcode

strings = [
    "MSTR98",
    "MSTR99",
    "P10000",
    "P10001",
    "P10002",
    "P10003",
    "P10004",
    "P10005",
    "P10006",
    "P10007",
    "P10008",
    "P10009",
    "P10010",
    "P10011",
    "P10012",
    "P10013",
    "P10014",
    "P10015"
]
for s in strings:
    img = qrcode.make(s)
    img.save(s + ".png")
