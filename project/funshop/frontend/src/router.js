
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import OrderManager from "./components/OrderManager"

import PaymentListManager from "./components/PaymentListManager"

import CartManager from "./components/CartManager"
import CancellationManager from "./components/CancellationManager"


import Mypage from "./components/Mypage"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/orders',
                name: 'OrderManager',
                component: OrderManager
            },

            {
                path: '/paymentLists',
                name: 'PaymentListManager',
                component: PaymentListManager
            },

            {
                path: '/carts',
                name: 'CartManager',
                component: CartManager
            },
            {
                path: '/cancellations',
                name: 'CancellationManager',
                component: CancellationManager
            },


            {
                path: '/mypages',
                name: 'Mypage',
                component: Mypage
            },


    ]
})
