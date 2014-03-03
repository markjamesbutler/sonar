define [
  'backbone',
  'cs!quality-gate/models/condition'
], (
  Backbone,
  Condition
) ->

  class Conditions extends Backbone.Collection
    model: Condition
