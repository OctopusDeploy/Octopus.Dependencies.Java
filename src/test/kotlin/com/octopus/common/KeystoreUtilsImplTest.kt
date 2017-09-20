package com.octopus.common

import com.octopus.calamari.utils.impl.KeystoreUtilsImpl
import org.junit.Test

const val EXAMPLE_PKCS1 = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIEpQIBAAKCAQEAw1a6hkjxXHgb9JpsnPSWoh4DzVePyqkKD2Odjb1Y3ctQ5/Ej\n" +
        "cwUJ8zYdPSL3IhjVnDxSvVJN/7wqNgChheGTimkdnCiYH2KELJsDhGCLCMaCfraI\n" +
        "nRhjmvLiYiY4TumPpSaLGZS1c1BEsy+Da8Zqok1ZBUqI0tpwolR9C37DszHlpi+R\n" +
        "BP363vIT1tIBbU/rwUYZtVlM8Mom6dhI+2JsRPq4sRhWLbD5lVELx1OKrmno55fF\n" +
        "TgrDQbmzFnJXplLSU42YzcresvvZXeAVWJ2C5x5l0bMTxy8OEw407Cdlzb07+5P9\n" +
        "uLTYO1dGXLh7F1JZKFfUn9Byf/6gQ03sw/YjpQIDAQABAoIBAQC+sGar+n8sHtBj\n" +
        "i3CmFBsMrbJWfbkQRM5eUYeXu5UKB5W2pu9NNEkPVO9cJEsOzSGSQE0hJjygfybR\n" +
        "yvHjSV+Hia/vJq3CTaDsDnNzge+YJHl1i2l3ujxaIesfl2H2nwisVyEJWuN7a4t1\n" +
        "RNfNmUe1oz5Y7Pb+p2G3BjV/yELxJPdYWnwmi4wpskLF42PCIphR/NsKApDoJa2Q\n" +
        "plC79CrWgsYdheFUvkhH2nEIqk5CSODhjHZQN0iJBbveJxX9UZqq8gl8i5dSzEmJ\n" +
        "uPJwRwFXngM+gwcE5ZJcP54ujjLdMmFVEmAMtoyWYb3IuJUIjoAEgK71+5qMJ6xf\n" +
        "aQCaZW9BAoGBAP6lYapOvJtl++eMM9nrKmc6G1skAl1gSPNT1ghR8z8KkrnLIECL\n" +
        "qTUE0diL960px8dQkvjhxQd6RRK5Hq+tkcqdZDk+Gts7tUy3CC6idZChoziFYeT0\n" +
        "8T7mZVlakLVmqu7S9emafhiTAItKo1FqJvrPcLsteMUJZzfOd6bqHGRVAoGBAMRg\n" +
        "npH4KABA2QZQL+h6yVe914fH1ClAFuaEV8ovqrscx/jAPEwvoHRhC4DEbkWwbbMf\n" +
        "3+LErHzIg87uA9S0xnTEwIWZwkTEkJ5i1T5dvAdz2TD4JlhqoDcyUVYl54dIQmqk\n" +
        "uSfkjP2OXDIwBiJmudhvZWEBdaybn7V60Etub5IRAoGASrv61+LX07uwnJo3NYOh\n" +
        "JD6XfL4qu4DgqBk4vaRfgWb2/PQyeP8EH/1UIPujKv0SFtr853JLQxCNaRtPeDu2\n" +
        "iAJ9QZeKhkEVyngFPLqNSm9F2fa7bli83Lr9j6XWxoZlMEZVycN/snKiPD8hg2lm\n" +
        "G0H6UdTcludVlblQwHoA7ykCgYEAj32GAGTSXbUEn09SYk3t9vXZmfZSuT2CPjfv\n" +
        "qeYAm65hFArrREP8u2z5qcJeTP+oeJ1Oy3UoEBm37ikOlYruA/6MKlL6l6MFhSX0\n" +
        "aRi2vr9QsS5xXmOy5AIZWphhwAD1vtTUEik3OEKgbW+X2+Ux4MssyZ/2awRfw4yU\n" +
        "zLlKPvECgYEA3kNMBqsZR+HV3wCZOuMb00YnkYwJv+jq+IEeoHXGZiEF7WQwgn4e\n" +
        "ZV9Kd0lY6EANBbxlt9bFiA8p4OJBBHvIYtKU4ymSetKAR+hebT/dMQDcadT+qXBp\n" +
        "XD+vCqUvr4CMYrhSXCbU6JRkXJAqjnoO7ER84eHQGuHgyeMvgTYeAMs=\n" +
        "-----END RSA PRIVATE KEY-----"

const val EXAMPLE_PKCS8 = "-----BEGIN PRIVATE KEY-----\n" +
        "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDDVrqGSPFceBv0\n" +
        "mmyc9JaiHgPNV4/KqQoPY52NvVjdy1Dn8SNzBQnzNh09IvciGNWcPFK9Uk3/vCo2\n" +
        "AKGF4ZOKaR2cKJgfYoQsmwOEYIsIxoJ+toidGGOa8uJiJjhO6Y+lJosZlLVzUESz\n" +
        "L4NrxmqiTVkFSojS2nCiVH0LfsOzMeWmL5EE/fre8hPW0gFtT+vBRhm1WUzwyibp\n" +
        "2Ej7YmxE+rixGFYtsPmVUQvHU4quaejnl8VOCsNBubMWclemUtJTjZjNyt6y+9ld\n" +
        "4BVYnYLnHmXRsxPHLw4TDjTsJ2XNvTv7k/24tNg7V0ZcuHsXUlkoV9Sf0HJ//qBD\n" +
        "TezD9iOlAgMBAAECggEBAL6wZqv6fywe0GOLcKYUGwytslZ9uRBEzl5Rh5e7lQoH\n" +
        "lbam7000SQ9U71wkSw7NIZJATSEmPKB/JtHK8eNJX4eJr+8mrcJNoOwOc3OB75gk\n" +
        "eXWLaXe6PFoh6x+XYfafCKxXIQla43tri3VE182ZR7WjPljs9v6nYbcGNX/IQvEk\n" +
        "91hafCaLjCmyQsXjY8IimFH82woCkOglrZCmULv0KtaCxh2F4VS+SEfacQiqTkJI\n" +
        "4OGMdlA3SIkFu94nFf1RmqryCXyLl1LMSYm48nBHAVeeAz6DBwTlklw/ni6OMt0y\n" +
        "YVUSYAy2jJZhvci4lQiOgASArvX7mownrF9pAJplb0ECgYEA/qVhqk68m2X754wz\n" +
        "2esqZzobWyQCXWBI81PWCFHzPwqSucsgQIupNQTR2Iv3rSnHx1CS+OHFB3pFErke\n" +
        "r62Ryp1kOT4a2zu1TLcILqJ1kKGjOIVh5PTxPuZlWVqQtWaq7tL16Zp+GJMAi0qj\n" +
        "UWom+s9wuy14xQlnN853puocZFUCgYEAxGCekfgoAEDZBlAv6HrJV73Xh8fUKUAW\n" +
        "5oRXyi+quxzH+MA8TC+gdGELgMRuRbBtsx/f4sSsfMiDzu4D1LTGdMTAhZnCRMSQ\n" +
        "nmLVPl28B3PZMPgmWGqgNzJRViXnh0hCaqS5J+SM/Y5cMjAGIma52G9lYQF1rJuf\n" +
        "tXrQS25vkhECgYBKu/rX4tfTu7Ccmjc1g6EkPpd8viq7gOCoGTi9pF+BZvb89DJ4\n" +
        "/wQf/VQg+6Mq/RIW2vzncktDEI1pG094O7aIAn1Bl4qGQRXKeAU8uo1Kb0XZ9rtu\n" +
        "WLzcuv2PpdbGhmUwRlXJw3+ycqI8PyGDaWYbQfpR1NyW51WVuVDAegDvKQKBgQCP\n" +
        "fYYAZNJdtQSfT1JiTe329dmZ9lK5PYI+N++p5gCbrmEUCutEQ/y7bPmpwl5M/6h4\n" +
        "nU7LdSgQGbfuKQ6Viu4D/owqUvqXowWFJfRpGLa+v1CxLnFeY7LkAhlamGHAAPW+\n" +
        "1NQSKTc4QqBtb5fb5THgyyzJn/ZrBF/DjJTMuUo+8QKBgQDeQ0wGqxlH4dXfAJk6\n" +
        "4xvTRieRjAm/6Or4gR6gdcZmIQXtZDCCfh5lX0p3SVjoQA0FvGW31sWIDyng4kEE\n" +
        "e8hi0pTjKZJ60oBH6F5tP90xANxp1P6pcGlcP68KpS+vgIxiuFJcJtTolGRckCqO\n" +
        "eg7sRHzh4dAa4eDJ4y+BNh4Ayw==\n" +
        "-----END PRIVATE KEY-----"

class KeystoreUtilsImplTest {
    @Test
    fun testPKCS1() {
        KeystoreUtilsImpl.createKey(EXAMPLE_PKCS1).get()
    }

    @Test
    fun testPKCS8() {
        KeystoreUtilsImpl.createKey(EXAMPLE_PKCS8).get()
    }
}