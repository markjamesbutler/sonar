requirejs.config({
  baseUrl: baseUrl + '/javascripts',

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars',
    'moment': 'third-party/moment',
    'select-list': 'common/select-list'
  },

  shim: {
    'backbone.marionette': {
      deps: ['backbone'],
      exports: 'Marionette'
    },
    'backbone': {
      exports: 'Backbone'
    },
    'handlebars': {
      exports: 'Handlebars'
    },
    'moment': {
      exports: 'moment'
    },
    'select-list': {
      exports: 'SelectList'
    }
  },

  waitSeconds: 0
});


requirejs(['cs!quality-gate/app'], function() { });
